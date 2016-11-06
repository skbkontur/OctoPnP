package ru.skbkontur.octopnp.agent;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

class NugetCommandBuilder {
    private final ArrayList<Map.Entry<String, Boolean>> args;

    NugetCommandBuilder(@NotNull final File workingDir) {
        args = new ArrayList<Map.Entry<String, Boolean>>();
        addArg(new File(workingDir, "nuget.exe").getAbsolutePath());
    }

    void addArg(@NotNull final String arg) {
        args.add(new AbstractMap.SimpleEntry(arg, false));
    }

    void addMaskableArg(@NotNull final String arg) {
        args.add(new AbstractMap.SimpleEntry(arg, true));
    }

    @NotNull
    String[] buildCommand() {
        return buildCommand(false);
    }

    @NotNull
    String[] buildMaskedCommand() {
        return buildCommand(true);
    }

    @NotNull
    private String[] buildCommand(boolean mask) {
        final String[] result = new String[args.size()];
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            Map.Entry arg = args.get(i);
            result[i] = mask && (Boolean)arg.getValue() ? "SECRET" : (String)arg.getKey();
        }
        return result;
    }
}
