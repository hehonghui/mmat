package com.mrcd.mmat.analyzer;

import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.util.IProgressListener;

public interface HprofAnalyzer {
    /**
     * 分析 hprof snapshot
     * @param snapshot
     */
    void analysis(ISnapshot snapshot, IProgressListener listener);
}
