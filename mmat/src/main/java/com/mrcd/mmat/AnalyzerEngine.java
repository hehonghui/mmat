package com.mrcd.mmat;

import com.mrcd.mmat.analyzer.BitmapAnalyzer;
import com.mrcd.mmat.analyzer.MemoryLeakAnalyzer;
import com.mrcd.mmat.android.AndroidOS;
import com.mrcd.mmat.report.HtmlTemplateReport;
import com.mrcd.mmat.util.FileUtils;
import com.mrcd.mmat.util.ProcessUtil;

import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * hprof 分析引起
 * create by mrsimple at 2019-08-03.
 */
class AnalyzerEngine {

    protected MemoryLeakAnalyzer mMemoryLeakAnalyzer;


    boolean start(File hprofFile, File jsonConfigFile, boolean disableMonkey) throws IOException, InterruptedException {
        return start(new AnalyzerArgs(hprofFile, jsonConfigFile, disableMonkey));
    }


    /**
     * 开始分析 hprof, 如果有必要的话会先执行 monkey 测试
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    boolean start(AnalyzerArgs args) throws IOException, InterruptedException {
        if ( args == null ) {
            return false;
        }
        // 清空 hprof_analysis 文件夹
        FileUtils.clearHeapDumpDir();

        final JSONObject jsonConfig = FileUtils.readJson(args.jsonConfigFile);
        final MMATConfig mmatConfig = new MMATConfig(jsonConfig);
        // 用户没有指定 hprof 文件的情况下才会根据配置判断是否需要运行 monkey 测试
        if (args.hprofFile == null) {
            // 1. 如果用户没有指定 -disable-monkey 则尝试执行monkey测试
            if (!args.disableMonkey) {
                runMonkey(mmatConfig);
            }
            // 2. dump hprof
            args.hprofFile = HprofDumper.dumpHprof(mmatConfig, args.disableMonkey);
        }
        System.out.println("Current dir : " + FileUtils.getRuntimeWorkDir().getAbsolutePath());

        final IProgressListener listener = new ConsoleProgressListener(System.out);
        // 3, 打开 hprof
        final ISnapshot snapshot = openHprof(args.hprofFile, listener);
        if (snapshot != null) {
            System.out.println(snapshot.getSnapshotInfo());
            // 4. 读取设备信息, 然后根据系统信息构建 exclude refs
            AndroidOS.parse(snapshot);
            mmatConfig.buildExcludeRefs(jsonConfig.optJSONArray("excluded_refs"));
            // 5. 分析 hprof
            analysisHprof(mmatConfig, snapshot, listener);
            // 6. 打开分析报告文件夹
            Desktop.getDesktop().open(FileUtils.getReportDir());
        }
        return true ;
    }

    private void runMonkey(MMATConfig config) {
        if (config != null && config.shouldRunMonkey()) {
            ProcessUtil.executeCommand(config.monkeyCommand);
        }
    }

    private ISnapshot openHprof(File hprofFile, IProgressListener listener) throws FileNotFoundException {
        if (!hprofFile.exists()) {
            throw new FileNotFoundException(String.format("%s file not found!", hprofFile.getAbsolutePath()));
        }
        System.out.println("opening hprof " + hprofFile.getAbsolutePath() + "...");
        ISnapshot snapshot = null;
        try {
            SnapshotFactory sf = new SnapshotFactory();
            // open hprof
            snapshot = sf.openSnapshot(hprofFile, new HashMap<String, String>(), listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    private void analysisHprof(MMATConfig analysisPackage, ISnapshot snapshot, IProgressListener listener) {
        final HtmlTemplateReport hprofReport = new HtmlTemplateReport();
        try {
            hprofReport.onStart();
            // analysis
            mMemoryLeakAnalyzer = new MemoryLeakAnalyzer(analysisPackage, hprofReport);
            mMemoryLeakAnalyzer.analysis(snapshot, listener);
            new BitmapAnalyzer(analysisPackage, hprofReport).analysis(snapshot, listener);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            // finish report
            hprofReport.onFinish();
        }
    }

    MemoryLeakAnalyzer getMemoryLeakAnalyzer() {
        return mMemoryLeakAnalyzer;
    }
}
