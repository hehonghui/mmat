package com.mrcd.mmat;

import java.io.File;

/**
 * create by mrsimple at 2019-08-03.
 */
class AnalyzerArgs {

    File hprofFile;
    File jsonConfigFile;
    boolean disableMonkey;

    AnalyzerArgs() {
    }

    AnalyzerArgs(File hprofFile, File jsonConfigFile, boolean disableMonkey) {
        this.hprofFile = hprofFile;
        this.jsonConfigFile = jsonConfigFile;
        this.disableMonkey = disableMonkey;
    }
}
