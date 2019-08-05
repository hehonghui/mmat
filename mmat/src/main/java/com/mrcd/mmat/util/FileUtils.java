package com.mrcd.mmat.util;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    private static final int TEN_MINUTES_MS = 10 * 60 * 1000 ;

    private FileUtils() {
    }

    public static File getReportDir() {
        final File reportFile = new File(getHeapDumpDir(), "report");
        if (!reportFile.exists()) {
            reportFile.mkdirs();
        }
        return reportFile;
    }

    public static File getHeapDumpDir() {
        final File reportFile = new File(getRuntimeWorkDir(), "hprof_analysis");
        if (!reportFile.exists()) {
            reportFile.mkdirs();
        }
        return reportFile;
    }


    public static File getRuntimeWorkDir() {
        final String dir = System.getProperty("user.dir");
        return new File(dir);
    }

    public static void clearDir(File dirOrFile) {
        if (dirOrFile.isDirectory()) {
            File[] files = dirOrFile.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    clearDir(files[i]);
                }
            }
        } else {
            dirOrFile.delete();
        }
    }

    public static void clearHeapDumpDir() {
        File dirOrFile = getHeapDumpDir();
        System.out.println("==> 清空 " + dirOrFile.getAbsolutePath());
        if (dirOrFile.isDirectory()) {
            File[] files = dirOrFile.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    clearDir(files[i]);
                }
            }
        } else {
            // 如果是十分钟之内生成的 hprof, 则不删除
            if ( dirOrFile.getAbsolutePath().endsWith(".hprof")
                    && System.currentTimeMillis() - dirOrFile.lastModified() <= TEN_MINUTES_MS) {
                return;
            }
            dirOrFile.delete();
        }
    }


    /**
     * @param file
     * @return
     */
    public static JSONObject readJson(File file) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file) ;
            return new JSONObject(read(inputStream)) ;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(inputStream);
        }
        return new JSONObject();
    }


    public static String read(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int byteRead = 0;
        while ((byteRead = inputStream.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, byteRead));
        }
        return sb.toString();
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
