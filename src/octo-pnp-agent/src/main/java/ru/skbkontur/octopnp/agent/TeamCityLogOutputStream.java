package ru.skbkontur.octopnp.agent;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.LoggingProcessListener;
import org.apache.commons.exec.LogOutputStream;
import org.jetbrains.annotations.NotNull;

class TeamCityLogOutputStream extends LogOutputStream {
    private final boolean isErrorLogger;
    private final LoggingProcessListener loggingProcessListener;

    TeamCityLogOutputStream(@NotNull final BuildProgressLogger logger, final boolean isErrorLogger) {
        this.isErrorLogger = isErrorLogger;
        loggingProcessListener = new LoggingProcessListener(logger);
    }

    @Override
    protected void processLine(final String line, final int level) {
        if (isErrorLogger) {
            loggingProcessListener.onErrorOutput(line);
        } else {
            loggingProcessListener.onStandardOutput(line);
        }
    }
}
