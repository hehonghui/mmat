package com.mrcd.mmat;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Command line arguments parse test case
 * create by mrsimple at 2019-08-03.
 */
public class MainCmdArgsTest {

    /**
     * show mmat usage help information
     * @throws Exception
     */
    @Test
    public void testShowPrintHelpWhenRun() throws Exception {
        String[] args = new String[]{"-h"};
        AnalyzerArgs cmdArgs = Main.parseArgs(args);
        assertNull(cmdArgs);

        args = new String[]{"-help"};
        cmdArgs = Main.parseArgs(args);
        assertNull(cmdArgs);
    }


    @Test
    public void noArguments() throws Exception {
        assertNull(Main.parseArgs(null));

        boolean error = false ;
        try {
            assertNull(Main.parseArgs(new String[]{}));
        } catch (Throwable e) {
            e.printStackTrace();
            error = true ;
        } finally {
            assertTrue(error);
        }
    }

    /**
     * specific mmat json config and disable monkey runner
     * @throws Exception
     */
    @Test
    public void parseValidJsonConfigArg() throws Exception {
        // json config file
        File jsonConfigFile = AssetsReader.open("mmat-config.json");
        String[] args = new String[]{jsonConfigFile.getAbsolutePath(), "-disable-monkey"};

        AnalyzerArgs analyzerArgs = Main.parseArgs(args);
        assertNotNull(analyzerArgs);

        // hprof file
        assertNull(analyzerArgs.hprofFile);
        // json config
        assertNotNull(analyzerArgs.jsonConfigFile);
        assertEquals(jsonConfigFile.getAbsolutePath(), analyzerArgs.jsonConfigFile.getAbsolutePath());
        // disable monkey runner before analysis hprof
        assertTrue(analyzerArgs.disableMonkey);
    }

    /**
     * specific hprof file and mmat json config file
     *
     * @throws Exception
     */
    @Test
    public void parseValidHprofFileAndJsonConfigArguments() throws Exception {
        // json config file
        File jsonConfigFile = AssetsReader.open("mmat-config.json");
        File hprofFile = AssetsReader.open("com.example.mmat.hprof");
        // arg0 : mmat json config, arg1: -hprof , arg2: hprof file path
        String[] args = new String[]{jsonConfigFile.getAbsolutePath(), "-hprof", hprofFile.getAbsolutePath()};

        AnalyzerArgs analyzerArgs = Main.parseArgs(args);
        assertNotNull(analyzerArgs);

        // hprof file
        assertNotNull(analyzerArgs.hprofFile);
        assertEquals(hprofFile.getAbsolutePath(), analyzerArgs.hprofFile.getAbsolutePath());
        // json config
        assertNotNull(analyzerArgs.jsonConfigFile);
        assertEquals(jsonConfigFile.getAbsolutePath(), analyzerArgs.jsonConfigFile.getAbsolutePath());

        assertFalse(analyzerArgs.disableMonkey);
    }
}
