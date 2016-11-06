package ru.skbkontur.octopnp.agent;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

class NugetCommandBuilder {
    private final File workingDir;
    private final ArrayList<Pair<String, Boolean>> args;

    NugetCommandBuilder(@NotNull final File workingDir) {
        this.workingDir = workingDir;
        args = new ArrayList<Pair<String, Boolean>>();
        addArg(new File(workingDir, "nuget.exe").getAbsolutePath());
    }

    void addArg(@NotNull final String arg) {
        args.add(new Pair(arg, false));
    }

    void addMaskableArg(@NotNull final String arg) {
        args.add(new Pair(arg, true));
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
    private String[] buildCommand(boolean masked) {
        final String[] result = new String[args.size()];
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            Pair<String, Boolean> arg = args.get(i);
            result[i] = masked && arg.getValue() ? "SECRET" : arg.getKey();
        }
        return result;
    }
}
