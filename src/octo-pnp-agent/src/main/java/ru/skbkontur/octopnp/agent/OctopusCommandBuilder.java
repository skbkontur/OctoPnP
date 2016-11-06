package ru.skbkontur.octopnp.agent;

import jetbrains.buildServer.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class OctopusCommandBuilder {
    public String[] buildCommand() {
        return buildCommand(false);
    }

    public String[] buildMaskedCommand() {
        return buildCommand(true);
    }

    protected abstract String[] buildCommand(boolean masked);

    protected String Quote(String value) {
        return "\"" + value + "\"";
    }

    protected List<String> splitSpaceSeparatedValues(String text) {
        List<String> results = new ArrayList<String>();
        if (text == null || StringUtil.isEmptyOrSpaces(text)) {
            return results;
        }

        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(text);
        while (m.find()) {
            String item = m.group(1).replace("\"", "");
            if (item != null && !item.isEmpty()) {
                results.add(item);
            }
        }

        return results;
    }

    protected List<String> splitCommaSeparatedValues(String text) {
        List<String> results = new ArrayList<String>();
        if (text == null || StringUtil.isEmptyOrSpaces(text)) {
            return results;
        }

        String line = text;
        String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String t : tokens) {
            String trimmed = t.trim();
            if (trimmed.length() > 0) {
                results.add(trimmed);
            }
        }

        return results;
    }
}
