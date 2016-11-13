package ru.skbkontur.octopnp.agent;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import org.apache.commons.exec.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

class TeamCityProcessCallable implements Callable<Long> {
    private static final Long KILLED_BY_WATCHDOG_EXIT_CODE = -999L;
    private final NugetCommandBuilder commandBuilder;
    private final BuildProgressLogger logger;
    private final String taskName;
    private final int watchdogTimeoutInSeconds;

    TeamCityProcessCallable(@NotNull final NugetCommandBuilder commandBuilder, @NotNull final BuildProgressLogger logger, @NotNull final String taskName, final int watchdogTimeoutInSeconds) {
        this.commandBuilder = commandBuilder;
        this.logger = logger;
        this.taskName = taskName;
        this.watchdogTimeoutInSeconds = watchdogTimeoutInSeconds;
    }

    @Override
    public Long call() throws Exception {
        logger.activityStarted(taskName, DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);
        Executor executor = new DefaultExecutor();
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        ExecuteWatchdog watchDog = new ExecuteWatchdog(watchdogTimeoutInSeconds * 1000);
        executor.setWatchdog(watchDog);
        TeamCityLogOutputStream outStream = new TeamCityLogOutputStream(logger, false);
        TeamCityLogOutputStream errStream = new TeamCityLogOutputStream(logger, true);
        executor.setStreamHandler(new PumpStreamHandler(outStream, errStream));
        Long exitCode;
        try {
            logger.message("Running command: " + commandBuilder.buildMaskedCommand());
            exitCode = new Long(executor.execute(commandBuilder.buildCommand()));
        } catch (ExecuteException e) {
            logger.exception(e);
            exitCode = new Long(e.getExitValue());
        }
        if (watchDog.killedProcess()) {
            exitCode = KILLED_BY_WATCHDOG_EXIT_CODE;
        }
        logger.message("Exit code: " + exitCode);
        logger.activityFinished(taskName, DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);
        return exitCode;
    }
}
