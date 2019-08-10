package com.mrcd.mmat.report;

public interface Reportable<T> {
    void onReport(T reportInfo);
    void onFinish();
}
