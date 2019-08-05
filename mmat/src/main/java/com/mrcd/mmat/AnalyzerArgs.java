package com.mrcd.mmat;

import java.io.File;

/**
 * create by mrsimple at 2019-08-03.
 */
class AnalyzerArgs {
    /**
     * json config absolute file path
     */
    File jsonConfigFile;
    /**
     * should we run monkey test before analysis hprof
     */
    boolean disableMonkey;
    /**
     * hprof file path
     */
    File hprofFile;

    AnalyzerArgs() {
    }

    AnalyzerArgs(File hprofFile, File jsonConfigFile, boolean disableMonkey) {
        this.hprofFile = hprofFile;
        this.jsonConfigFile = jsonConfigFile;
        this.disableMonkey = disableMonkey;
    }

    @Override
    public String toString() {
        return "AnalyzerArgs{" +
                "jsonConfigFile=" + jsonConfigFile +
                ", disableMonkey=" + disableMonkey +
                ", hprofFile=" + hprofFile +
                '}';
    }
}
