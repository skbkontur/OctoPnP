package ru.skbkontur.octopnp.agent;

import java.io.*;
import java.lang.*;

public class EmbeddedResourceExtractor {
    public void extractNugetTo(String destinationPath) throws Exception {
        extractFile("/nuget.exe", destinationPath + "\\nuget.exe");
    }

    private void extractFile(String resourceName, String destinationName) throws Exception {
        int attempts = 0;
        while (true) {
            attempts++;

            try {
                File file = new File(destinationName);
                if (file.exists())
                    return;

                InputStream is = getClass().getResourceAsStream(resourceName);
                OutputStream os = new FileOutputStream(destinationName, false);

                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }

                os.close();
                is.close();
            }
            catch (Exception ex) {
                Thread.sleep(4000);
                if (attempts > 3) {
                    throw ex;
                }
            }
        }
    }
}
