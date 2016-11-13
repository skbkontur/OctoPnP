package ru.skbkontur.octopnp.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.skbkontur.octopnp.CommonConstants;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

class PackAndPublishBuildProcess implements BuildProcess, Callable<BuildFinishedStatus> {
    private final Logger LOG = Logger.getInstance(getClass().getName());
    private final BuildProgressLogger logger;
    private final boolean failBuildOnExitCode;
    private final File checkoutDir;
    private final File workingDir;
    private final Map<String, String> runnerParameters;
    private final CommonConstants octopnpConstants;
    private List<String> nuspecFiles;
    private Future<BuildFinishedStatus> buildProcessFuture;

    PackAndPublishBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext buildRunnerContext) {
        logger = runningBuild.getBuildLogger();
        failBuildOnExitCode = runningBuild.getFailBuildOnExitCode();
        checkoutDir = runningBuild.getCheckoutDirectory();
        workingDir = new File(runningBuild.getBuildTempDirectory(), "octopnpTmp");
        runnerParameters = buildRunnerContext.getRunnerParameters();
        octopnpConstants = new CommonConstants();
    }

    public void interrupt() {
        LOG.info("BuildProcess interrupt");
        buildProcessFuture.cancel(true);
    }

    public boolean isInterrupted() {
        return buildProcessFuture.isCancelled() && isFinished();
    }

    public boolean isFinished() {
        return buildProcessFuture.isDone();
    }

    public void start() throws RunBuildException {
        final String nuspecPaths = runnerParameters.get(octopnpConstants.getNuspecPathsKey());
        nuspecFiles = resolveNuspecAbsoluteFileNames(nuspecPaths);
        resetWorkingDir();
        extractNugetExe();
        try {
            buildProcessFuture = Executors.newSingleThreadExecutor().submit(this);
            LOG.info("BuildProcess started");
        } catch (final RejectedExecutionException e) {
            String message = "BuildProcess couldn't start";
            LOG.error(message, e);
            throw new RunBuildException(message, e);
        }
    }

    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {
        try {
            final BuildFinishedStatus status = buildProcessFuture.get();
            LOG.info("BuildProcess finished");
            if (status.isFailed()) {
                logger.buildFailureDescription("Unable to pack or publish some release. Please check the build log for details.");
            }
            return status;
        } catch (final ExecutionException e) {
            String message = "BuildProcess failed";
            LOG.error(message, e);
            throw new RunBuildException(message, e);
        } catch (final InterruptedException e) {
            String message = "BuildProcess thread was interrupted";
            LOG.error(message, e);
            throw new RunBuildException(message, e);
        } catch (final CancellationException e) {
            LOG.info("BuildProcess was interrupted", e);
            return BuildFinishedStatus.INTERRUPTED;
        }
    }

    @Override
    public BuildFinishedStatus call() throws Exception {
        final List<Future<Long>> packExitCodes = runPackCommands();
        for (Future<Long> exitCode : packExitCodes) {
            if (failBuildOnExitCode && exitCode.get() != 0) {
                return BuildFinishedStatus.FINISHED_FAILED;
            }
        }
        final List<Future<Long>> pushExitCodes = runPushCommands();
        for (Future<Long> exitCode : pushExitCodes) {
            if (failBuildOnExitCode && exitCode.get() != 0) {
                return BuildFinishedStatus.FINISHED_FAILED;
            }
        }
        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    @NotNull
    private List<Future<Long>> runPackCommands() throws InterruptedException {
        final int watchdogTimeoutInSeconds = NugetCommandBuilder.timeoutInSeconds + 5;
        final ArrayList<TeamCityProcessCallable> packTasks = new ArrayList<TeamCityProcessCallable>(nuspecFiles.size());
        for (int i = 0; i < nuspecFiles.size(); i++) {
            final String nuspecFile = nuspecFiles.get(i);
            final Path relativeNuspecPath = Paths.get(checkoutDir.getAbsolutePath()).relativize(Paths.get(nuspecFile));
            final String taskName = "Pack " + String.valueOf(i + 1) + "/" + String.valueOf(nuspecFiles.size()) + ": " + relativeNuspecPath;
            final BuildProgressLogger taskLogger = logger.getFlowLogger(nuspecFile);
            final NugetCommandBuilder commandBuilder = createNugetPackCommandBuilder(nuspecFile);
            packTasks.add(new TeamCityProcessCallable(commandBuilder, taskLogger, taskName, watchdogTimeoutInSeconds));
        }
        final List<Future<Long>> packExitCodes = Executors.newFixedThreadPool(8).invokeAll(packTasks);
        return packExitCodes;
    }

    @NotNull
    private List<Future<Long>> runPushCommands() throws InterruptedException, RunBuildException {
        File[] nupkgFiles = workingDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nupkg");
            }
        });
        if (nupkgFiles == null || nupkgFiles.length == 0) {
            throw new RunBuildException("There are no packages to push");
        }
        final int watchdogTimeoutInSeconds = NugetCommandBuilder.timeoutInSeconds + 5;
        final ArrayList<TeamCityProcessCallable> pushTasks = new ArrayList<TeamCityProcessCallable>(nupkgFiles.length);
        for (int i = 0; i < nupkgFiles.length; i++) {
            final String nupkgFile = nupkgFiles[i].getAbsolutePath();
            final String taskName = "Push " + String.valueOf(i + 1) + "/" + String.valueOf(nupkgFiles.length) + ": " + nupkgFiles[i].getName();
            final BuildProgressLogger taskLogger = logger.getFlowLogger(nupkgFile);
            final NugetCommandBuilder commandBuilder = createNugetPushCommandBuilder(nupkgFile);
            pushTasks.add(new TeamCityProcessCallable(commandBuilder, taskLogger, taskName, watchdogTimeoutInSeconds));
        }
        final List<Future<Long>> pushExitCodes = Executors.newFixedThreadPool(pushTasks.size()).invokeAll(pushTasks);
        return pushExitCodes;
    }

    @NotNull
    private NugetCommandBuilder createNugetPackCommandBuilder(@NotNull final String nuspecFile) {
        String packageVersion = null;
        final String packageVersionKey = octopnpConstants.getPackageVersionKey();
        if (runnerParameters.containsKey(packageVersionKey)) {
            packageVersion = runnerParameters.get(packageVersionKey);
        }
        return NugetCommandBuilder.forPack(workingDir, nuspecFile, packageVersion);
    }

    @NotNull
    private NugetCommandBuilder createNugetPushCommandBuilder(@NotNull final String nupkgFile) {
        final String octopusApiKey = runnerParameters.get(octopnpConstants.getOctopusApiKey());
        final String octopusServerUrl = runnerParameters.get(octopnpConstants.getOctopusServerUrlKey());
        return NugetCommandBuilder.forPush(workingDir, nupkgFile, octopusApiKey, octopusServerUrl);
    }

    private void extractNugetExe() throws RunBuildException {
        try {
            new EmbeddedResourceExtractor().extractNugetTo(workingDir.getAbsolutePath());
        } catch (Exception e) {
            throw new RunBuildException("Unable to extract nuget.exe into " + workingDir, e);
        }
    }

    private void resetWorkingDir() throws RunBuildException {
        try {
            FileUtils.deleteDirectory(workingDir);
        } catch (Exception e) {
            throw new RunBuildException("Unable to delete working directory " + workingDir, e);
        }
        if (!workingDir.mkdirs())
            throw new RunBuildException("Unable to create working directory " + workingDir);
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