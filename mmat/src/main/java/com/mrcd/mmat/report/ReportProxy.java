package com.mrcd.mmat.report;

/**
 * report 代理类, 真正输出报告的是 {@link HtmlTemplateReport}
 * @param <T>
 */
abstract class ReportProxy<T> implements Reportable<T> {

    protected final Reportable mHtmlReporter ;

    protected ReportProxy(Reportable htmlReporter) {
        mHtmlReporter = htmlReporter;
    }

    protected void writeLine(String line) {
        if (mHtmlReporter != null) {
            mHtmlReporter.onReport(line);
        }
    }

    protected void writeDivider() {
        writeLine("<DIV style=\"BORDER-TOP: #999999 1px dashed; OVERFLOW: hidden; HEIGHT: 1px\"></DIV>");
    }

    protected void nothingFound(String tips) {
        writeLine(String.format("<div><h2  class=\"success\" align=\"center\">%s</h2></div>", tips));
    }
}
