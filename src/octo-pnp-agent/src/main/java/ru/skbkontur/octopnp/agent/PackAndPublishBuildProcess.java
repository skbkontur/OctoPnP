package ru.skbkontur.octopnp.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.runner.LoggingProcessListener;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringUtils;
import ru.skbkontur.octopnp.CommonConstants;

import java.io.*;
import java.util.*;

class PackAndPublishBuildProcess implements BuildProcess {
    private final BuildProgressLogger logger;
    private final boolean failBuildOnExitCode;
    private final File checkoutDir;
    private final File workingDir;
    private final Map<String, String> runnerParameters;
    private final CommonConstants octopnpConstants;
    private OutputReaderThread standardError;
    private OutputReaderThread standardOutput;
    private Process nugetProcess;
    private boolean isFinished;

    PackAndPublishBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext buildRunnerContext) {
        logger = runningBuild.getBuildLogger();
        failBuildOnExitCode = runningBuild.getFailBuildOnExitCode();
        checkoutDir = runningBuild.getCheckoutDirectory();
        workingDir = new File(runningBuild.getBuildTempDirectory(), "octopnpTmp");
        runnerParameters = buildRunnerContext.getRunnerParameters();
        octopnpConstants = new CommonConstants();
    }

    public boolean isInterrupted() {
        return false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void interrupt() {
        if (nugetProcess != null) {
            nugetProcess.destroy();
        }
    }

    public void start() throws RunBuildException {
        final String nuspecPaths = runnerParameters.get(octopnpConstants.getNuspecPathsKey());
        final List<String> nuspecFiles = resolveNuspecAbsoluteFileNames(nuspecPaths);
        resetWorkingDir();
        extractNugetExe();
        for (String nuspecFile : nuspecFiles) {
            NugetCommandBuilder packCmdBuilder = createNugetPackCommandBuilder(nuspecFile);
            runNuget(packCmdBuilder);
        }

        File[] nupkgFiles = workingDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nupkg");
            }
        });
        for (File nupkgFile : nupkgFiles) {
            NugetCommandBuilder pushCmdBuilder = createNugetPushCommandBuilder(nupkgFile.getAbsolutePath());
            runNuget(pushCmdBuilder);
        }
    }

    private void runNuget(@NotNull final NugetCommandBuilder cmdBuilder) throws RunBuildException {
        logger.activityStarted("Nuget.exe", DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);
        logger.message("Running command: " + StringUtils.arrayToDelimitedString(cmdBuilder.buildMaskedCommand(), " "));
        try {
            Runtime runtime = Runtime.getRuntime();
            nugetProcess = runtime.exec(cmdBuilder.buildCommand(), null, workingDir);
            final LoggingProcessListener listener = new LoggingProcessListener(logger);
            standardError = new OutputReaderThread(nugetProcess.getErrorStream(), new OutputWriter() {
                public void write(String text) {
                    listener.onErrorOutput(text);
                }
            });
            standardOutput = new OutputReaderThread(nugetProcess.getInputStream(), new OutputWriter() {
                public void write(String text) {
                    listener.onStandardOutput(text);
                }
            });
            standardError.start();
            standardOutput.start();
        } catch (IOException e) {
            final String message = "Failed to run nuget.exe: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
    }

    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {
        int exitCode;
        try {
            exitCode = nugetProcess.waitFor();
            standardError.join();
            standardOutput.join();
            logger.message("nuget.exe exit code: " + exitCode);
            logger.activityFinished("Nuget.exe", DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);
            isFinished = true;
        } catch (InterruptedException e) {
            isFinished = true;
            final String message = "Unable to wait for nuget.exe: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }

        if (exitCode == 0)
            return BuildFinishedStatus.FINISHED_SUCCESS;

        logger.progressFinished();
        String message = "Unable to create or deploy release. Please check the build log for details on the error.";

        if (failBuildOnExitCode) {
            logger.buildFailureDescription(message);
            return BuildFinishedStatus.FINISHED_FAILED;
        } else {
            logger.error(message);
            return BuildFinishedStatus.FINISHED_SUCCESS;
        }
    }

    @NotNull
    private NugetCommandBuilder createNugetPackCommandBuilder(@NotNull final String nuspecFile) {
        String packageVersion = null;
        final String packageVersionKey = octopnpConstants.getPackageVersionKey();
        if (runnerParameters.containsKey(packageVersionKey)) {
            packageVersion = runnerParameters.get(packageVersionKey);
        }

        NugetCommandBuilder builder = new NugetCommandBuilder(workingDir);
        builder.addArg("pack");
        builder.addArg(nuspecFile);
        builder.addArg("-NonInteractive");
        builder.addArg("-NoPackageAnalysis");
        if (!StringUtil.isEmptyOrSpaces(packageVersion)) {
            builder.addArg("-Version");
            builder.addArg(packageVersion);
        }
        builder.addArg("-OutputDirectory");
        builder.addArg(workingDir.getAbsolutePath());
        return builder;
    }

    @NotNull
    private NugetCommandBuilder createNugetPushCommandBuilder(@NotNull final String nupkgFile) {
        final String octopusApiKey = runnerParameters.get(octopnpConstants.getOctopusApiKey());
        final String octopusServerUrl = runnerParameters.get(octopnpConstants.getOctopusServerUrlKey());

        NugetCommandBuilder builder = new NugetCommandBuilder(workingDir);
        builder.addArg("push");
        builder.addArg(nupkgFile);
        builder.addArg("-NonInteractive");
        builder.addArg("-Source");
        builder.addArg(octopusServerUrl);
        builder.addArg("-ApiKey");
        builder.addMaskableArg(octopusApiKey);
        builder.addArg("-Timeout");
        builder.addArg("300");
        return builder;
    }

    private void extractNugetExe() throws RunBuildException {
        try {
            new EmbeddedResourceExtractor().extractNugetTo(workingDir.getAbsolutePath());
        } catch (Exception e) {
            final String message = "Unable to extract nuget.exe into " + workingDir + ": " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
    }

    private void resetWorkingDir() throws RunBuildException {
        try {
            FileUtils.deleteDirectory(workingDir);
        } catch (Exception e) {
            final String message = "Unable to delete temp working directory " + workingDir + ": " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
        if (!workingDir.mkdirs())
            throw new RuntimeException("Unable to create temp working directory " + workingDir);
    }

    @NotNull
    private List<String> resolveNuspecAbsoluteFileNames(@Nullable final String nuspecPaths) throws RunBuildException {
        logger.message("Resolving nuspecPaths: " + nuspecPaths);
        final List<File> files = AntPatternFileCollector.scanDir(checkoutDir, splitFileWildcards(nuspecPaths), new String[0], null);
        logger.message("Matched .nuspec files:");
        final List<String> result = new ArrayList<String>(files.size());
        if (files.size() == 0) {
            logger.message("  none");
        } else {
            for (File file : files) {
                final String absoluteFileName = file.getAbsolutePath();
                result.add(absoluteFileName);
                logger.message("  " + absoluteFileName);
            }
        }
        if (result.size() == 0) {
            throw new RunBuildException("No files matched the pattern: " + nuspecPaths);
        }
        return result;
    }

    @NotNull
    private static String[] splitFileWildcards(@Nullable final String string) {
        if (string == null) return new String[0];
        final String filesStringWithSpaces = string.replace('\n', ' ').replace('\r', ' ').replace('\\', '/');
        final List<String> split = StringUtil.splitCommandArgumentsAndUnquote(filesStringWithSpaces);
        return split.toArray(new String[split.size()]);
    }
}


