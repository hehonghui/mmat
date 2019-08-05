package com.mrcd.mmat.plugin

/**
 * create by mrsimple at 2019-08-05.
 */
public class MmatExtension {
    /**
     * json config absolute file path
     */
    String jsonConfigFile = ""
    /**
     * should we run monkey test before analysis hprof
     */
    boolean disableMonkey = false
    /**
     * hprof file path
     */
    String hprofFile = ""

    @Override
    public String toString() {
        return "MmatExtension{" +
                "jsonConfigFile='" + jsonConfigFile + '\'' +
                ", disableMonkey=" + disableMonkey +
                ", hprofFile='" + hprofFile + '\'' +
                '}'
    }
}
