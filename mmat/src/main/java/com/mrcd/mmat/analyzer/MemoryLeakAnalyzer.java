package com.mrcd.mmat.analyzer;

import com.mrcd.mmat.MMATConfig;
import com.mrcd.mmat.analyzer.trace.LeakTrace;
import com.mrcd.mmat.analyzer.trace.LeakTraceTracker;
import com.mrcd.mmat.report.MemoryLeakReport;
import com.mrcd.mmat.report.Reportable;
import com.mrcd.mmat.util.CollectionsUtil;

import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.IProgressListener;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Activity、Fragment等类型的内存泄漏分析
 */
public class MemoryLeakAnalyzer implements HprofAnalyzer {
    /**
     * 要分析的应用信息
     */
    protected final MMATConfig mAnalysisConfig;
    /**
     * leak reporter
     */
    protected final MemoryLeakReport mLeakReport;
    /**
     * leak trance records
     */
    protected List<LeakTrace> mLeakTraces = new LinkedList<>() ;

    public MemoryLeakAnalyzer(MMATConfig mmatConfig, Reportable htmlReport) {
        this.mAnalysisConfig = mmatConfig ;
        this.mLeakReport = new MemoryLeakReport(htmlReport, mmatConfig.packageName);
    }

    /**
     * analysis activity and fragment's memory leak
     *
     * @param snapshot
     * @param listener
     */
    @Override
    public void analysis(ISnapshot snapshot, IProgressListener listener) {
        try {
            for (String name : mAnalysisConfig.detectedClasses) {
                Collection<IClass> classes = snapshot.getClassesByName(name, true);
                if (CollectionsUtil.isEmpty(classes)) {
                    System.out.println(String.format(
                            "Cannot find class %s in heap dump", name));
                    continue;
                }
                // find all target classed
                Iterator<IClass> classIterator = classes.iterator();
                while (classIterator.hasNext()) {
                    IClass clazz = classIterator.next();
                    int[] objIds = clazz.getObjectIds();
                    long minRetainedSize = snapshot.getMinRetainedSize(objIds, listener);
                    if (minRetainedSize == 0) {
                        continue;
                    }

                    for (int i = 0; i < objIds.length; i++) {
                        final int objId = objIds[i];
                        IObject object = snapshot.getObject(objId);
                        // find gc root
                        findMemoryLeak(snapshot, object);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLeakReport.onFinish();
        }
    }

    private void findMemoryLeak(ISnapshot snapshot, IObject leakedObj) throws Exception {
        LeakTrace leakTraceResult = new LeakTraceTracker().findLeakTrace(snapshot, leakedObj, mAnalysisConfig.excludedRefs);
        // report leak info
        if ( mLeakReport != null && leakTraceResult != null ) {
            mLeakTraces.add(leakTraceResult) ;
            mLeakReport.onReport(leakTraceResult);
        }
    }

    public List<LeakTrace> getLeakTraces() {
        return mLeakTraces;
    }
}
