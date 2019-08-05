package com.mrcd.mmat;

import com.mrcd.mmat.util.FileUtils;
import com.mrcd.mmat.util.ProcessUtil;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * dump hprof file
 * create by mrsimple at 2019-06-14.
 */
class HprofDumper {

    private static final int BACK_PRESS_KEY = 4 ;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss") ;


    private HprofDumper() {
    }

    /**
     * 应用退到后台, 然后force gc, 最后dump hprof
     * @param config
     * @param disableMonkey
     * @return
     * @throws InterruptedException
     */
    static File dumpHprof(MMATConfig config, boolean disableMonkey) throws InterruptedException {
        // 只有在运行了monkey测试的情况下才需要跳转到主页面, 并且执行 force-gc
        if ( config.shouldRunMonkey() && !disableMonkey) {
            // dump meminfo来获取到进程的pid
            ProcessUtil.executeCommand("adb shell dumpsys meminfo " + config.packageName) ;
            // 跳转到应用的主页面, 并且清理掉其他页面 (确保除了主页面的其他页面都应该销毁, 如果没有销毁, 那么则是产生了内存泄漏)
            ProcessUtil.executeCommand(String.format("adb shell am start -n %s/%s", config.packageName, config.mainActivity)) ;
            // wait
            Thread.sleep(5000);

            // 发送两次后退事件, 使得应用退到后台
            ProcessUtil.executeCommand("adb shell input keyevent " + BACK_PRESS_KEY) ;
            ProcessUtil.executeCommand("adb shell input keyevent " + BACK_PRESS_KEY) ;
            if ( config.enableForceGc && ProcessUtil.getPid() != ProcessUtil.INVALID_PID ) {
                // force gc (需要root设备)
                ProcessUtil.executeCommand(String.format("adb shell su -c \"kill -10 %d\"", ProcessUtil.getPid())) ;
            } else {
                System.out.println("Disable force gc !");
            }
            // wait
            Thread.sleep(5000);
        }

        File hprofFile = tryDumpHprof(config, config.hprofStorageDir);
        // 使用 sdcard 路径dump失败时, 尝试使用 /data/local/tmp/ 路径保存 hprof
        if ( (!hprofFile.exists() || hprofFile.length() <= 0 )
                && MMATConfig.DEFAULT_HPROF_DIR.equalsIgnoreCase(config.hprofStorageDir) ) {
            System.out.println("try to dump hprof in " + MMATConfig.ANDROID_LOCAL_TMP_DIR);
            hprofFile = tryDumpHprof(config, MMATConfig.ANDROID_LOCAL_TMP_DIR) ;
        }
        return hprofFile ;
    }

    private static File tryDumpHprof(MMATConfig config, String hprofStorageDir) throws InterruptedException {
        String hprofName = String.format("%s-dump-%s.hprof", config.packageName, DATE_FORMAT.format(new Date())) ;
        // hprof file path
        final String hprofFilePath = hprofStorageDir + hprofName ;
        // dump hprof
        ProcessUtil.executeCommand(String.format("adb shell am dumpheap %s %s", config.packageName, hprofFilePath)) ;
        Thread.sleep(config.dumpHprofWaitTime * 1000);

        // pull hprof to report dir
        ProcessUtil.executeCommand("adb pull " + hprofFilePath + " " + FileUtils.getHeapDumpDir().getAbsolutePath()) ;
        Thread.sleep(config.pullHprofWaitTime * 1000);
        // delete hprof in sdcard
        ProcessUtil.executeCommand(String.format("adb shell rm %s", hprofFilePath)) ;
        // hprof file
        return new File(FileUtils.getHeapDumpDir().getAbsolutePath(), hprofName) ;
    }
}
