package com.mrcd.mmat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {

    private static int sPid ;

    private ProcessUtil() {
    }

    public static String executeCommand(String command) {
        System.out.println("execute command line : " + command);
        String line;
        StringBuilder sb = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        BufferedReader bufferedReader = null ;
        try {
            Process process = runtime.exec(command);
            bufferedReader = new BufferedReader
                    (new InputStreamReader(process.getInputStream()));
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line + "\n");
                System.out.println(line);
                if ( line.contains("pid") ) {
                    sPid = parsePid(line) ;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeQuietly(bufferedReader);
        }
        return sb.toString() ;
    }

    public static int parsePid(String line) {
        String[] parts = line.split(" ") ;
        return Integer.valueOf(parts[4]) ;
    }

    public static int getPid() {
        return sPid ;
    }
}
