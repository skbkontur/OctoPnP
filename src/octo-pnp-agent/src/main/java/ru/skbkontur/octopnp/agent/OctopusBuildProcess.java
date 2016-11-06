package ru.skbkontur.octopnp.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.runner.LoggingProcessListener;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class OctopusBuildProcess implements BuildProcess {
    protected final AgentRunningBuild runningBuild;
    protected final BuildRunnerContext context;
    private Process process;
    protected File extractedTo;
    private OutputReaderThread standardError;
    private OutputReaderThread standardOutput;
    private boolean isFinished;
    protected BuildProgressLogger logger;

    protected OctopusBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
        this.runningBuild = runningBuild;
        this.context = context;
    }

    public void start() throws RunBuildException {
    }

    protected abstract String getLogMessage();

    protected BuildRunnerContext getContext() {
        return context;
    }

    protected void extractNugetExe() throws RunBuildException {
        final File tempDirectory = runningBuild.getBuildTempDirectory();
        try {
            extractedTo = new File(tempDirectory, "octopnpTmp");
            if (!extractedTo.exists()) {
                if (!extractedTo.mkdirs())
                    throw new RuntimeException("Unable to create temp output directory " + extractedTo);
            }

            EmbeddedResourceExtractor extractor = new EmbeddedResourceExtractor();
            extractor.extractNugetTo(extractedTo.getAbsolutePath());
        } catch (Exception e) {
            final String message = "Unable to create temporary file in " + tempDirectory + " for OctoPnP: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
    }

    protected void runNuget(final OctopusCommandBuilder command) throws RunBuildException {
        String[] userVisibleCommand = command.buildMaskedCommand();
        String[] realCommand = command.buildCommand();

        logger = runningBuild.getBuildLogger();
        logger.activityStarted("OctoPnP", DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);
        logger.message("Running command:   nuget.exe " + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
        logger.progressMessage(getLogMessage());

        try {
            Runtime runtime = Runtime.getRuntime();

            ArrayList<String> arguments = new ArrayList<String>();
            arguments.add(new File(extractedTo, "\\nuget.exe").getAbsolutePath());
            arguments.addAll(Arrays.asList(realCommand));

            process = runtime.exec(arguments.toArray(new String[arguments.size()]), null, context.getWorkingDirectory());

            final LoggingProcessListener listener = new LoggingProcessListener(logger);

            standardError = new OutputReaderThread(process.getErrorStream(), new OutputWriter() {
                public void write(String text) {
                    listener.onErrorOutput(text);
                }
            });
            standardOutput = new OutputReaderThread(process.getInputStream(), new OutputWriter() {
                public void write(String text) {
                    listener.onStandardOutput(text);
                }
            });

            standardError.start();
            standardOutput.start();
        } catch (IOException e) {
            final String message = "Error from nuget.exe: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
    }

    public boolean isInterrupted() {
        return false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void interrupt() {
        if (process != null) {
            process.destroy();
        }
    }

    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {
        int exitCode;

        try {
            exitCode = process.waitFor();

            standardError.join();
            standardOutput.join();

            logger.message("nuget.exe exit code: " + exitCode);
            logger.activityFinished("OctoPnP", DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);

            isFinished = true;
        }
        catch (InterruptedException e) {
            isFinished = true;
            final String message = "Unable to wait for nuget.exe: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }

        if (exitCode == 0)
            return BuildFinishedStatus.FINISHED_SUCCESS;

        runningBuild.getBuildLogger().progressFinished();

        String message = "Unable to create or deploy release. Please check the build log for details on the error.";

        if (runningBuild.getFailBuildOnExitCode()) {
            runningBuild.getBuildLogger().buildFailureDescription(message);
            return BuildFinishedStatus.FINISHED_FAILED;
        } else {
            runningBuild.getBuildLogger().error(message);
            return BuildFinishedStatus.FINISHED_SUCCESS;
        }
    }

    private interface OutputWriter {
        void write(String text);
    }

    private class OutputReaderThread extends Thread {
        private final InputStream is;
        private final OutputWriter output;

        OutputReaderThread(InputStream is, OutputWriter output) {
            this.is = is;
            this.output = output;
        }

        public void run() {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            try {
                while ((line = br.readLine()) != null) {
                    output.write(line.replaceAll("[\\r\\n]", ""));
                }
            } catch (IOException e) {
                output.write("ERROR: " + e.getMessage());
            }
        }
    }
}
