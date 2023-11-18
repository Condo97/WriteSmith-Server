package com.writesmith.apple.iapvalidation;

import java.io.*;

public class KeyReader {

    public static String readKeyFromFile(String fileDirectory) throws IOException {
        // Get file from path
        File file = new File(fileDirectory);

        // Read using BufferedReader
        BufferedReader br = new BufferedReader(new FileReader(file));

        String output = "";
        while ((output += br.readLine()) != null);

        return output;
    }

}
