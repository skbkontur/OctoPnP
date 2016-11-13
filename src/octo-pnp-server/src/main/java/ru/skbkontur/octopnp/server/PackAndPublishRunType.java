package ru.skbkontur.octopnp.server;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.skbkontur.octopnp.CommonConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PackAndPublishRunType extends RunType {
    private final PluginDescriptor pluginDescriptor;

    public PackAndPublishRunType(final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
        runTypeRegistry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return CommonConstants.OCTOPNP_PACK_AND_PUBLISH_RUNNER_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "OctoPnP: Pack and Publish";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Create Octopus-compatible NuGet-packages and publish them to Octopus";
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        final CommonConstants c = new CommonConstants();
        return new PropertiesProcessor() {
            private void checkNotEmpty(@NotNull final Map<String, String> properties,
                                       @NotNull final String key,
                                       @NotNull final String message,
                                       @NotNull final Collection<InvalidProperty> res) {
                if (jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(properties.get(key))) {
                    res.add(new InvalidProperty(key, message));
                }
            }

            @NotNull
            public Collection<InvalidProperty> process(@Nullable final Map<String, String> p) {
                final Collection<InvalidProperty> result = new ArrayList<>();
                if (p != null) {

                    checkNotEmpty(p, c.getOctopusApiKey(), "Octopus API key must be specified", result);
                    checkNotEmpty(p, c.getOctopusServerUrlKey(), "Octopus server URL must be specified", result);
                    checkNotEmpty(p, c.getNuspecPathsKey(), "Nuspec paths must be specified", result);
                }
                return result;
            }
        };
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("editPackAndPublish.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("viewPackAndPublish.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<>();
    }
}
