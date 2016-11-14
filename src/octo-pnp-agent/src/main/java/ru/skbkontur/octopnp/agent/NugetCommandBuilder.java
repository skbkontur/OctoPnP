package ru.skbkontur.octopnp.agent;

import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.exec.CommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

class NugetCommandBuilder {
    final static int timeoutInSeconds = 300;
    private final File executable;
    private final ArrayList<Map.Entry<String, Boolean>> args;

    private NugetCommandBuilder(@NotNull final File workingDir) {
        executable = new File(workingDir, "nuget.exe");
        args = new ArrayList<>();
    }

    @NotNull
    static NugetCommandBuilder forPack(@NotNull final File workingDir, @NotNull final String nuspecFile, @Nullable final String packageVersion) {
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
    static NugetCommandBuilder forPush(@NotNull final File workingDir, @NotNull final String nupkgFile, @NotNull final String octopusApiKey, @NotNull final String pushToUrl) {
        NugetCommandBuilder builder = new NugetCommandBuilder(workingDir);
        builder.addArg("push");
        builder.addArg(nupkgFile);
        builder.addArg("-NonInteractive");
        builder.addArg("-Source");
        builder.addArg(pushToUrl);
        builder.addArg("-ApiKey");
        builder.addMaskableArg(octopusApiKey);
        builder.addArg("-Timeout");
        builder.addArg(String.valueOf(timeoutInSeconds));
        return builder;
    }

    private void addArg(@NotNull final String arg) {
        args.add(new AbstractMap.SimpleEntry<>(arg, false));
    }

    private void addMaskableArg(@NotNull final String arg) {
        args.add(new AbstractMap.SimpleEntry<>(arg, true));
    }

    @NotNull
    CommandLine buildCommand() {
        return buildCommand(false);
    }

    @NotNull
    CommandLine buildMaskedCommand() {
        return buildCommand(true);
    }

    @NotNull
    private CommandLine buildCommand(boolean mask) {
        final CommandLine result = new CommandLine(executable);
        for (Map.Entry<String, Boolean> arg : args) {
            result.addArgument(mask && arg.getValue() ? "SECRET" : arg.getKey());
        }
        return result;
    }
}
