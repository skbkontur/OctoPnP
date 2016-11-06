package ru.skbkontur.octopnp.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class OutputReaderThread extends Thread {
    private final InputStream is;
    private final OutputWriter output;

    OutputReaderThread(InputStream is, OutputWriter output) {
        this.is = is;
        this.output = output;
    }

    public void run() {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        try {
            while ((line = br.readLine()) != null) {
                output.write(line.replaceAll("[\\r\\n]", ""));
            }
        } catch (IOException e) {
            output.write("ERROR: " + e.getMessage());
        }
    }
}
