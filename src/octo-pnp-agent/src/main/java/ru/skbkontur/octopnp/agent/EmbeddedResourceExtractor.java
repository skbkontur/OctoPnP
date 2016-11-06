package ru.skbkontur.octopnp.agent;

import java.io.*;
import java.lang.*;

class EmbeddedResourceExtractor {
    void extractNugetTo(String destinationPath) throws Exception {
        extractFileIfNotExists("/nuget.exe", destinationPath + "\\nuget.exe");
    }

    private void extractFileIfNotExists(String resourceName, String destinationName) throws Exception {
        File file = new File(destinationName);
        if (file.exists())
            return;

        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is == null)
            throw new Exception("Resource " + resourceName + " was not found");

        OutputStream os = new FileOutputStream(destinationName, false);

        byte[] buffer = new byte[4096];
        int length;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }

        os.close();
        is.close();
    }
}
