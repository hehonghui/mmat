package com.mrcd.mmat;

import com.mrcd.mmat.analyzer.trace.AndroidExcludeRefs;
import com.mrcd.mmat.analyzer.trace.ExcludedRefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Dimension;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MMATConfig {

    /**
     * 默认的hprof 文件dump到手机中的路径
     */
    static final String DEFAULT_HPROF_DIR = "/sdcard/";
    /**
     * local temp dir
     */
    static final String ANDROID_LOCAL_TMP_DIR = "/data/local/tmp/";

    /**
     * 应用包名
     */
    public final String packageName;
    /**
     * 应用的main activity, 需要在 AndroidManifest.xml 总设置 exported=true
     */
    public final String mainActivity;
    /**
     * monkey 测试配置, 可以配置命令和文件
     */
    public final String monkeyCommand;
    /**
     * 要检测内存泄漏的类列表 (包括其子类)
     */
    public final List<String> detectedClasses = new LinkedList<>();
    /**
     * 内存泄漏排除的列列表 (例如系统内存泄漏, 只有弱引用、软引用的对象)
     */
    public ExcludedRefs excludedRefs = null;
    /**
     * dump的hprof存储在Android设备中的路径, 默认为 /sdcard/ , android 9.0 之后adb不能直接访问 /sdcard/,
     * 因此添加了可配置项.
     */
    final String hprofStorageDir;
    /**
     * bitmap report config
     */
    public final BitmapReportConfig bitmapConfig;
    /**
     * 开启force gc 功能 (配合跑monkey的时候dump hprof之前force gc)
     */
    final boolean enableForceGc;
    /**
     * dump hprof 时等待多少秒, 默认为15秒
     */
    final int dumpHprofWaitTime ;
    /**
     * pull hprof 时等待多少秒, 默认为15秒
     */
    final int pullHprofWaitTime ;

    MMATConfig(JSONObject config) {
        // 1. base info
        packageName = config.optString("package");
        mainActivity = config.optString("main_activity");
        // monkey config
        monkeyCommand = config.optString("monkey_command");
        enableForceGc = config.optBoolean("enable_force_gc", true);

        String hprofDir = config.optString("hprof_dir", DEFAULT_HPROF_DIR);
        if (!hprofDir.endsWith("/")) {
            hprofDir = hprofDir + File.separator;
        }
        this.hprofStorageDir = hprofDir;
        this.dumpHprofWaitTime = config.optInt("dump_hprof_wait_time", 15) ;
        this.pullHprofWaitTime = config.optInt("pull_hprof_wait_time", 15) ;

        // 2. detect leak classes
        JSONArray detectLeakClassesArray = config.optJSONArray("detect_leak_classes");
        if (detectLeakClassesArray != null) {
            for (int i = 0; i < detectLeakClassesArray.length(); i++) {
                detectedClasses.add(detectLeakClassesArray.optString(i));
            }
        }
        // 3. parse bitmap report
        JSONObject bitmapConfig = config.optJSONObject("bitmap_report");
        this.bitmapConfig = new BitmapReportConfig(bitmapConfig != null ? bitmapConfig : new JSONObject());
    }

    public boolean shouldRunMonkey() {
        return monkeyCommand != null && monkeyCommand.length() > 0;
    }

    /**
     * @param excludedArray
     * @return
     */
    void buildExcludeRefs(JSONArray excludedArray) {
        ExcludedRefs.Builder builder = new ExcludedRefs.Builder();
        // If the FinalizerWatchdogDaemon thread is on the shortest path, then there was no other
        // reference to the object and it was about to be GCed.
        builder.thread("FinalizerWatchdogDaemon");
        // The main thread stack is ever changing so local variables aren't likely to hold references
        // for long. If this is on the shortest path, it's probably that there's a longer path with
        // a real leak.
        builder.thread("main");

        // parse exclude classes and fields
        if (excludedArray != null) {
            for (int i = 0; i < excludedArray.length(); i++) {
                parseExcludeRef(builder, excludedArray.optJSONObject(i));
            }
        }
        // system memory leak
        AndroidExcludeRefs.buildSystemExcludeRefs(builder);
        excludedRefs = builder.build();
    }

    /**
     * {
     *      "class": "java.lang.ref.WeakReference",
     *      "fields": ["referent"],
     *      "type": "instance"          // type : instance or static
     * }
     *
     * @param excludeItem
     * @return
     */
    private static void parseExcludeRef(ExcludedRefs.Builder builder, JSONObject excludeItem) {
        if (excludeItem == null) {
            return;
        }
        final String className = excludeItem.optString("class");
        final String type = excludeItem.optString("type");
        final JSONArray fields = excludeItem.optJSONArray("fields");
        if (fields != null && fields.length() > 0) {
            for (int i = 0; i < fields.length(); i++) {
                final String fieldName = fields.optString(i);
                if (ExcludedRefs.STATIC_FIELD_LEAK.equalsIgnoreCase(type)) {
                    builder.staticField(className, fieldName);
                } else {
                    builder.instanceField(className, fieldName);
                }
            }
        }
    }


    /**
     * bitmap report config
     */
    public static class BitmapReportConfig {
        /**
         * default report bitmap size
         */
        private static final int DEFAULT_SIZE = 200;
        /**
         * 宽度最小是多少时才会report
         */
        protected final int mMinWidth;
        /**
         * 高度最小是多少时才会report
         */
        protected final int mMinHeight;
        /**
         * 最多report多少张图片, -1 表示不设限制
         */
        protected final int mMaxReportCount;

        public BitmapReportConfig(JSONObject bitmapConfig) {
            mMaxReportCount = bitmapConfig.optInt("max_report_count", -1);
            mMinWidth = bitmapConfig.optInt("min_width", DEFAULT_SIZE);
            mMinHeight = bitmapConfig.optInt("min_height", DEFAULT_SIZE);
        }

        public boolean isOverflow(int index) {
            return mMaxReportCount >= 0 && index >= mMaxReportCount;
        }

        public boolean shouldReport(Dimension dimension) {
            return shouldReport(dimension.width, dimension.height);
        }

        public boolean shouldReport(int width, int height) {
            return width >= mMinWidth || height >= mMinHeight;
        }
    } // end of BitmapConfig
}
