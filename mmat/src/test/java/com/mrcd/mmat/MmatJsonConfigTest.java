package com.mrcd.mmat;

import com.mrcd.mmat.util.FileUtils;

import org.json.JSONObject;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class MmatJsonConfigTest {

    @Test
    public void testParseConfig() {
        System.out.println(FileUtils.getRuntimeWorkDir());
        File mmatConfigFile = AssetsReader.open("mmat-config.json");
        assertTrue(mmatConfigFile.exists());

        JSONObject jsonObject = FileUtils.readJson(mmatConfigFile) ;
        assertNotNull(jsonObject);

        MMATConfig mmatConfig = new MMATConfig(jsonObject) ;
        assertEquals("com.example.mmat", mmatConfig.packageName);
        assertEquals("com.example.mmat.MainActivity", mmatConfig.mainActivity);
        assertEquals(15, mmatConfig.dumpHprofWaitTime);
        assertEquals(15, mmatConfig.pullHprofWaitTime);
        assertEquals(true, mmatConfig.enableForceGc);
        assertEquals("/sdcard/", mmatConfig.hprofStorageDir);
        assertTrue( mmatConfig.monkeyCommand.startsWith("adb shell monkey"));

        assertEquals(3, mmatConfig.detectedClasses.size());
        assertEquals("android.app.Activity", mmatConfig.detectedClasses.get(0));
        assertEquals("android.app.Fragment", mmatConfig.detectedClasses.get(1));
        assertEquals("android.support.v4.app.Fragment", mmatConfig.detectedClasses.get(2));

        assertEquals(20, mmatConfig.bitmapConfig.mMaxReportCount);
        assertEquals(256, mmatConfig.bitmapConfig.mMinWidth);
        assertEquals(256, mmatConfig.bitmapConfig.mMinHeight);
    }
}
