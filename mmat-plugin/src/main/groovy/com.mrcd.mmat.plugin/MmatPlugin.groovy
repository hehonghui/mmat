package com.mrcd.mmat.plugin

import com.mrcd.mmat.AnalyzerEngine
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 参考: https://juejin.im/post/5a523dd56fb9a01cbf382ce9
 * 参考:
 *
 * 调试插件请参考: https://blog.csdn.net/ceabie/article/details/55271161
 */
public class MmatPlugin implements Plugin<Project> {

    MmatExtension mmatExt

    void apply(Project project) {
        try {
            System.out.println("\n\n\n========================")
            System.out.println("config mmat plugin!")
            System.out.println("========================")

            project.extensions.create('mmat', MmatExtension)
            mmatExt = project.mmat

            // create mmatRunner task : startMmatRunner
            project.task("startMmatRunner", group: 'verification') {
                doLast {
                    startMmatRunner()
                }
            }

            // create mmatRunner task : startMmatSilently
            project.task("startMmatSilently", group: 'verification') {
                doLast {
                    mmatExt.disableMonkey = true
                    startMmatRunner()
                }
            }
        } catch (Throwable e) {
            e.printStackTrace()
        }
    }

    void startMmatRunner() {
        System.out.println("current dir : " + new File("./").getAbsolutePath())
        File jsonConfigFile
        if ( TextUtils.isEmpty(mmatExt.jsonConfigFile) ) {
            // default mmat config
            jsonConfigFile = new File("./mmat-config.json")
        } else {
            jsonConfigFile = new File(mmatExt.jsonConfigFile)
        }

        System.out.println("json config path : " + mmatExt.jsonConfigFile)
        if ( !jsonConfigFile.exists() ) {
            throw new FileNotFoundException("json config file not found!!")
        }

        File hprofFile = null
        if ( TextUtils.isNotEmpty( mmatExt.hprofFile) ) {
            hprofFile = new File(mmatExt.hprofFile)
        }
        try {
            System.out.println("mmat config : " + mmatExt)
            new AnalyzerEngine().start( (hprofFile != null && hprofFile.exists()) ? hprofFile : null,
                                        jsonConfigFile, mmatExt.disableMonkey)
        } catch (Throwable e) {
            e.printStackTrace()
        }
    }
}