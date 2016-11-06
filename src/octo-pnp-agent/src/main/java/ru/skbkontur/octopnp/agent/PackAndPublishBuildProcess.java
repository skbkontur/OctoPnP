package ru.skbkontur.octopnp.agent;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import org.jetbrains.annotations.NotNull;
import ru.skbkontur.octopnp.CommonConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackAndPublishBuildProcess extends OctopusBuildProcess {

    protected final ExtensionHolder myExtensionHolder;
    protected final AgentRunningBuild myRunningBuild;

    public PackAndPublishBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull final ExtensionHolder extensionHolder) {
       super(runningBuild, context);

        myExtensionHolder = extensionHolder;
        myRunningBuild = runningBuild;
    }

    @Override
    protected String getLogMessage() {
        return "Packing and publishing deployment packages to Octopus server";
    }

    @Override
    public void start() throws RunBuildException {

        final Map<String, String> parameters = getContext().getRunnerParameters();
        final CommonConstants constants = new CommonConstants();
        final String nuspecPaths = parameters.get(constants.getNuspecPathsKey());

        getLogger().logMessage(DefaultMessagesInfo.createTextMessage("nuspecPaths: " + nuspecPaths));
        final List<String> nuspecFiles = matchFiles(nuspecPaths);
        if (nuspecFiles.size() == 0) {
            throw new RunBuildException("No files matched the pattern");
        }

        extractNugetExe();
        OctopusCommandBuilder arguments = createCommand(nuspecFiles);
        runNuget(arguments);
    }

    protected OctopusCommandBuilder createCommand(final List<String> nuspecFiles) {
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final CommonConstants constants = new CommonConstants();

        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String serverUrl = parameters.get(constants.getOctopusServerUrlKey());
                final String apiKey = parameters.get(constants.getOctopusApiKey());

                String packageVersion = null;
                final String packageVersionKey = constants.getPackageVersionKey();
                if (parameters.containsKey(packageVersionKey)) {
                    packageVersion = parameters.get(packageVersionKey);
                }

                commands.add("pack");
                for (String nuspecFile : nuspecFiles) {
                        commands.add(nuspecFile);
                }
                commands.add("-NoPackageAnalysis");
                if (!StringUtil.isEmptyOrSpaces(packageVersion))
                {
                    commands.add("-Version");
                    commands.add(packageVersion);
                }
                commands.add("-OutputDirectory");
                commands.add(extractedTo.getAbsolutePath());

                /*commands.add("--server");
                commands.add(serverUrl);
                commands.add("--apikey");
                commands.add(masked ? "SECRET" : apiKey);*/

                return commands.toArray(new String[commands.size()]);
            }
        };
    }

    private List<String> matchFiles(String nuspecPaths) {
        final List<File> files = AntPatternFileCollector.scanDir(getCheckoutDirectory(), splitFileWildcards(nuspecPaths), new String[0], null);

        getLogger().logMessage(DefaultMessagesInfo.createTextMessage("Matched .nuspec files:"));

        final List<String> result = new ArrayList<String>(files.size());
        if (files.size() == 0) {
            getLogger().logMessage(DefaultMessagesInfo.createTextMessage("  none"));
        } else {
            for (File file : files) {
                final String relativeName = FileUtil.getRelativePath(getWorkingDirectory(), file);

                result.add(relativeName);
                getLogger().logMessage(DefaultMessagesInfo.createTextMessage("  " + relativeName));
            }
        }

        return result;
    }

    private static String[] splitFileWildcards(final String string) {
        if (string != null) {
            final String filesStringWithSpaces = string.replace('\n', ' ').replace('\r', ' ').replace('\\', '/');
            final List<String> split = StringUtil.splitCommandArgumentsAndUnquote(filesStringWithSpaces);
            return split.toArray(new String[split.size()]);
        }

        return new String[0];
    }

    @NotNull
    public final BuildProgressLogger getLogger() {
        return this.runningBuild.getBuildLogger();
    }

    @NotNull
    protected final File getWorkingDirectory() {
        return this.runningBuild.getBuildTempDirectory();
    }

    public File getCheckoutDirectory() {
        return this.runningBuild.getCheckoutDirectory();
    }
}
