package com.mrcd.mmat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {

    public static final int INVALID_PID = -1;

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
        final String[] parts = line.split(" ") ;
        int pid;
        try {
            pid = Integer.valueOf(parts[4]);
        } catch (Throwable e) {
            e.printStackTrace();
            pid = findPid(parts) ;
        }
        return pid;
    }

    public static int findPid(String[] parts) {
        int pid = INVALID_PID ;
        if ( parts == null || parts.length == 0 ) {
            return pid ;
        }

        for (int i = 0; i < parts.length; i++) {
            if ( parts[i] != null ) {
                try {
                    pid = Integer.valueOf(parts[i]) ;
                    if ( pid != INVALID_PID ) {
                        break;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return pid ;
    }

    public static int getPid() {
        return sPid ;
    }
}
