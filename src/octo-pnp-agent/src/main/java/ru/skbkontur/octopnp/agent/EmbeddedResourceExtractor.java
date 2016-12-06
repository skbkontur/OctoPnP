package ru.skbkontur.octopnp.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

class EmbeddedResourceExtractor {
    void extractNugetTo(String destinationPath) throws Exception {
        extractFileIfNotExists("/nuget.exe", destinationPath + "/nuget.exe");
    }

    private void extractFileIfNotExists(String resourceName, String destinationName) throws Exception {
        File file = new File(destinationName);
        if (file.exists())
            return;
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null)
                throw new Exception("Resource was not found: " + resourceName);
            try (OutputStream os = new FileOutputStream(destinationName, false)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) >= 0)
                    os.write(buffer, 0, length);
            }
        }
    }
}
