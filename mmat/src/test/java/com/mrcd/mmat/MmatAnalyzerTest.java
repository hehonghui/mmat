package com.mrcd.mmat;

import com.mrcd.mmat.analyzer.trace.LeakTrace;
import com.mrcd.mmat.analyzer.trace.LeakTraceElement;
import com.mrcd.mmat.util.ProcessUtil;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class MmatAnalyzerTest {

    /**
     * test analysis of valid hprof
     */
    @Test
    public void testAnalysisHprof() {
        AnalyzerEngine engine = new AnalyzerEngine();
        File hprofFile = AssetsReader.open("com.example.mmat.hprof");
        File jsonConfigFile = AssetsReader.open("mmat-config.json") ;
        // 启动分析引起
        try {
            engine.start(hprofFile, jsonConfigFile , true);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotNull( engine.getMemoryLeakAnalyzer());
        // verify memory leak records
        final List<LeakTrace> leakTraces = engine.getMemoryLeakAnalyzer().getLeakTraces() ;
        // 12 memory leaks in the hprof
        assertEquals(12 , leakTraces.size());


        // 1. first leak instance
        LeakTrace leakTrace1 = leakTraces.get(0) ;
        assertEquals(5, leakTrace1.elements.size());
        LeakTraceElement element1 = leakTrace1.getLeakedElement() ;
        assertEquals("com.example.mmat.MainActivity", element1.className);
        assertEquals("0x12f04958", element1.address);
        // dominate element
        assertEquals("android.widget.LinearLayout", leakTrace1.getDominateElement().className);
        assertEquals("mContext", leakTrace1.getDominateElement().referenceName);
        // other leak element of MainActivity
        assertEquals("com.android.internal.policy.DecorView", leakTrace1.elements.get(2).className);
        assertEquals("android.view.inputmethod.InputMethodManager", leakTrace1.elements.get(1).className);
        assertEquals("android.view.inputmethod.InputMethodManager$1", leakTrace1.elements.get(0).className);


        // 2. second leak instance
        LeakTrace leakTrace2 = leakTraces.get(1) ;
        LeakTraceElement element2 = leakTrace2.getLeakedElement() ;
        assertEquals("com.example.mmat.MemoryLeakActivity", element2.className);
        assertEquals("0x13203ff0", element2.address);

        // dominate element of MemoryLeakActivity
        assertEquals("java.util.LinkedList$Node", leakTrace2.getDominateElement().className);
        assertEquals("item", leakTrace2.getDominateElement().referenceName);

        // other leak element of MemoryLeakActivity
        assertEquals("java.util.LinkedList", leakTrace2.elements.get(4).className);
        assertEquals("com.example.mmat.MemoryLeakActivity", leakTrace2.elements.get(3).className);
        assertEquals("java.lang.Object[]", leakTrace2.elements.get(2).className);
        assertEquals("dalvik.system.PathClassLoader", leakTrace2.elements.get(1).className);
        assertEquals("java.lang.Thread", leakTrace2.elements.get(0).className);
    }

    @Test
    public void testHprofFileNotFound() {
        boolean notFound = false ;
        AnalyzerEngine engine = new AnalyzerEngine();
        File hprofFile = AssetsReader.open("not_found.hprof");
        File jsonConfigFile = AssetsReader.open("mmat-config.json") ;
        // 启动分析引起
        try {
            engine.start(hprofFile, jsonConfigFile , true);
        } catch (IOException e) {
            e.printStackTrace();
            notFound = true ;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(notFound);
        assertNull(engine.getMemoryLeakAnalyzer());
    }


    @Test
    public void testJsonConfigFileNotFound() {
        boolean notFound = false ;
        AnalyzerEngine engine = new AnalyzerEngine();
        File hprofFile = AssetsReader.open("com.example.mmat.hprof");
        File jsonConfigFile = AssetsReader.open("not_found.json") ;
        // 启动分析引起
        try {
            engine.start(hprofFile, jsonConfigFile , true);
        } catch (IOException e) {
            e.printStackTrace();
            notFound = true ;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(notFound);
        assertNotNull(engine.getMemoryLeakAnalyzer());
        assertEquals(0, engine.getMemoryLeakAnalyzer().getLeakTraces().size());
    }


    @Test
    public void testParsePID() {
        String pidLine = "** MEMINFO in pid 31736 [com.example.demo] **";
        assertEquals(31736, ProcessUtil.parsePid(pidLine));
    }
}