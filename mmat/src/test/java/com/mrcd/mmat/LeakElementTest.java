package com.mrcd.mmat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * create by mrsimple at 2019-06-14.
 */
public class LeakElementTest {

    @Test
    public void testAppendLeakInfo() {
        StringBuilder sb = new StringBuilder() ;
        String className = "com.mrcd.Actiivty";
        sb.append(className) ;
        String referenceName = "mContext";
        if (referenceName != null) {
            sb.insert(0, referenceName);
            sb.insert(referenceName.length(), " -> ");
        }
        assertEquals(referenceName + " -> " + className, sb.toString());
    }
}
