/*
 * Copyright 2005-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.mrcd.mmat;

import com.mrcd.mmat.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Examines an Android heap dump.
 * 
 * This MAT core code is adapted from <a href="https://bitbucket.org/ekabanov/mat/">
 * bitbucket.org/ekabanov/mat/</a>, which is a stripped-down command line
 * version of <a href="http://www.eclipse.org/mat/">Eclipse Memory Analyzer</a>.
 *
 */
public class Main {
    /**
     * disable monkey
     */
    static final String DISABLE_MONKEY = "-disable-monkey";

    private static void usage(String message) {
        if (message != null) {
            System.err.println("ERROR: " + message);
        }

        System.err.println("============================== MMAT Usage ===============================");
        System.err.println("Usage: java  -jar  mmat.jar  [-h|-help]  <mmat-json-config>  [-hprof] [hprof-file-path] [-disable-monkey]");
        System.err.println();
        System.err.println("\t-h|-help              show help information");
        System.err.println("\tmmat-json-config      mmat json file absolute path");
        System.err.println("\t-hprof                means the user will set hprof file(Optional), if so MMAT will not run monkey test and dump hprof automatically ");
        System.err.println("\thprof-file-path       hprof file absolute path, the path must be set after -hprof argument.");
        System.err.println("\t-disable-monkey       disable monkey test");
        System.err.println("=======================================================================");
    }


    /**
     * 1. 运行monkey测试 (如果monkey开关是开启状态的话)
     * 2. 跳转到应用主页面 (main activity需要在AndroidManifest.xml声明时添加上 exported=true )
     * 3. 回到桌面, 使得所有Activity、Fragment 都销毁
     * 4. force gc (设备需要root)
     * 5. dump hprof
     * 6. 自动分析内存泄漏和Bitmap
     * 7. 输出报告
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        AnalyzerArgs cmdArgs = parseArgs(args) ;
        // 开始进行hprof分析
        new AnalyzerEngine().start(cmdArgs);
    }

    /**
     * parse command line arguments
     * @param args
     * @return
     * @throws FileNotFoundException
     */
    static AnalyzerArgs parseArgs(String[] args) throws FileNotFoundException {
        if ( args == null ) {
            return null;
        }
        final AnalyzerArgs analyzerArgs = new AnalyzerArgs() ;
        // parse args
        for (int i = 0; i < args.length; i++) {
            final String curArg = args[i] ;
            // print help information
            if ("-h".equals(curArg) || "-help".equals(curArg)) {
                usage(null);
                return null ;
            }

            // 用户指定 hprof 文件的路径时不会再执行 monkey 测试, 直接进行分析
            if ("-hprof".equalsIgnoreCase(curArg) && args.length > ( i + 1 ) ) {
                File hprofFile = new File(args[i + 1]) ;
                if ( !hprofFile.exists() ) {
                    throw new FileNotFoundException(String.format("%s file not found!", hprofFile.getAbsolutePath())) ;
                }
                analyzerArgs.hprofFile = hprofFile ;
            }

            // json config arg
            if ( curArg.endsWith(".json") ) {
                analyzerArgs.jsonConfigFile = new File(curArg) ;
            }

            // disable monkey
            if ( DISABLE_MONKEY.equalsIgnoreCase(curArg) ) {
                analyzerArgs.disableMonkey = true ;
            }
        }

        // 确认 mmat json config 参数, 如果未设置则使用当前路径下的mmat-config.json
        if ( analyzerArgs.jsonConfigFile == null ) {
            // 如果没有参数, 则从当前目录下找 mmat-config.json 文件
            analyzerArgs.jsonConfigFile = new File(FileUtils.getRuntimeWorkDir() + File.separator + "mmat-config.json") ;
        }

        if ( !analyzerArgs.jsonConfigFile.exists() ) {
            throw new FileNotFoundException("mmat-config.json not found!") ;
        }
        return analyzerArgs ;
    }
}
