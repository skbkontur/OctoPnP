package ru.skbkontur.octopnp.agent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import ru.skbkontur.octopnp.CommonConstants;

public class PackAndPublishRunner implements AgentBuildRunner {
    private static final Logger LOG = Loggers.SERVER;
    protected final ExtensionHolder myExtensionHolder;

    public PackAndPublishRunner(@NotNull final ExtensionHolder extensionHolder) {
        myExtensionHolder = extensionHolder;
    }

    @NotNull
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) throws RunBuildException {
        return new PackAndPublishBuildProcess(runningBuild, context, myExtensionHolder);
    }

    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new AgentBuildRunnerInfo() {
            @NotNull
            public String getType() {
                return CommonConstants.OCTOPNP_PACK_AND_PUBLISH_RUNNER_TYPE;
            }

            public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
                if (!agentConfiguration.getSystemInfo().isWindows()) {
                    LOG.debug(getType() + " runner is supported only under Windows platform");
                    return false;
                }
                return true;
            }
        };
    }
}
