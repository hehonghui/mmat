package com.mrcd.mmat.report;

import com.mrcd.mmat.util.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * html 报告输出, 这里只是负责构建 html 的文件头部和尾部, 真正写入内容的是 {@link BitmapReport} 和 {@link MemoryLeakReport}
 */
public class HtmlTemplateReport implements Reportable<String> {

    private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    protected BufferedWriter mReportWriter;
    protected boolean isHeaderWrote = false;

    /**
     * 开始分析hprof.
     */
    public void onStart() {
        try {
            mReportWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(generateReportFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeHtmlHeadersIfNeed();
    }

    protected File generateReportFile() throws IOException {
        File leakReportFile = new File(FileUtils.getReportDir(), "leak_report_" + TIME_FORMATTER.format(new Date()) + ".html");
        if (leakReportFile.exists()) {
            leakReportFile.delete();
        } else {
            leakReportFile.createNewFile();
        }
        return leakReportFile;
    }


    protected void writeHtmlHeadersIfNeed() {
        if (isHeaderWrote) {
            return;
        }
        isHeaderWrote = true;
        onReport("<!DOCTYPE html>");
        onReport("<html>");
        onReport("<head>");
        // css style
        onReport("    " + getCssStyle());
        onReport("    <meta charset=\"utf-8\">");
        onReport("    <title>Hprof Analysis Report</title>");
        onReport("</head>");
        onReport("<body>");
    }

    @Override
    public void onReport(String line) {
        if (mReportWriter != null) {
            try {
                mReportWriter.write(line);
                mReportWriter.write("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFinish() {
        onReport("</body></html>");
        FileUtils.closeQuietly(mReportWriter);
    }


    private static String getCssStyle() {
        String cssContent = "";
        InputStream inputStream = null;
        try {
            inputStream = HtmlTemplateReport.class.getClassLoader().getResourceAsStream("report.css");
            cssContent = FileUtils.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeQuietly(inputStream);
        }
        return cssContent ;
    }
}

