package com.mrcd.mmat.report;

import com.mrcd.mmat.analyzer.trace.LeakTrace;
import com.mrcd.mmat.android.AndroidOS;

/**
 * make a memory report body
 */
public class MemoryLeakReport extends ReportProxy<LeakTrace> {

    protected int mLeakCount = 0;

    public MemoryLeakReport(Reportable htmlReporter, String packageName) {
        super(htmlReporter);

        writeLine(String.format("<div><h1 align=\"center\">Memory Report For %s</h1></div>", packageName));
        if ( AndroidOS.MODEL != null && AndroidOS.MODEL.length() > 0 ) {
            writeLine(String.format("<div><h6 align=\"center\">( %s : Android %s , API %d )</h6></div>", AndroidOS.MODEL, AndroidOS.RELEASE, AndroidOS.SDK_INT));
        }
    }

    @Override
    public void onReport(LeakTrace leakTrace) {
        if (leakTrace == null || leakTrace.isEmpty()) {
            return;
        }
        System.out.println(leakTrace.format(false));
        if (mHtmlReporter != null) {
            writeLeakRecord(leakTrace);
        }
        writeDivider();
    }

    protected void writeLeakRecord(LeakTrace leakTrace) {
        mLeakCount++;
        writeLine("<div class=\"leak_class\">");
        writeLine("<pre class=\"leak_code\">");
        writeLine("<h2> " + mLeakCount + ". " + leakTrace.getLeakedElement() + "</h2>");
        writeLine(leakTrace.format(true));
        writeLine("</pre>");
    }

    @Override
    public void onFinish() {
        if ( mLeakCount == 0 ) {
            System.out.println("\n=================== NO MEMORY LEAK ================\n");
            nothingFound("No Memory leak!");
        }
    }
}
