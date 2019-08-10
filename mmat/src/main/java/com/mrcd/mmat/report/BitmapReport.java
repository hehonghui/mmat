package com.mrcd.mmat.report;

import com.mrcd.mmat.MMATConfig;
import com.mrcd.mmat.analyzer.BitmapAnalyzer;
import com.mrcd.mmat.android.AndroidOS;
import com.mrcd.mmat.android.AndroidVersions;
import com.mrcd.mmat.bitmap.HprofBitmapProvider;
import com.mrcd.mmat.util.MemorySizeFormat;

import java.awt.Dimension;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Bitmap report proxy
 */
public class BitmapReport extends ReportProxy<HprofBitmapProvider> {
    /**
     * report records
     */
    protected List<HprofBitmapProvider> mBitmapRecords = new LinkedList<>() ;

    protected final MMATConfig.BitmapReportConfig mReportConfig ;

    public BitmapReport(MMATConfig.BitmapReportConfig config, Reportable htmlReporter) {
        super(htmlReporter);
        this.mReportConfig = config ;
        writeLine("<div class=\"bitmap_h2\" ><h1 align=\"center\">Bitmap Summary</h1></div>");
    }

    @Override
    public void onReport(HprofBitmapProvider reportInfo) {
        if ( reportInfo != null && reportInfo.getPixelBytes().length > 0 ) {
            mBitmapRecords.add(reportInfo) ;
        }
    }

    /**
     * 分析完成之后对所有图片按照内存占用大小排序, 然后输出html报告
     */
    @Override
    public void onFinish() {
        if ( mBitmapRecords.size() == 0 ) {
            if ( AndroidOS.SDK_INT >= AndroidVersions.O ) {
                nothingFound("Can't read bitmap data in java heap on Android 8.0 and above!");
            } else {
                nothingFound("No Bitmap found!");
            }
            return;
        }
        Collections.sort(mBitmapRecords, new Comparator<HprofBitmapProvider>() {
            @Override
            public int compare(HprofBitmapProvider left, HprofBitmapProvider right) {
                return right.getPixelBytes().length - left.getPixelBytes().length;
            }
        });

        for (int i = 0; i < mBitmapRecords.size(); i++) {
            if ( mReportConfig.isOverflow(i) ) {
                break;
            }
            final HprofBitmapProvider item = mBitmapRecords.get(i) ;
            if (  mReportConfig.shouldReport(item.getDimension() ) ) {
                reportRecord(item, i);
            }
        }
    }

    protected void reportRecord(HprofBitmapProvider reportInfo, int index) {
        Dimension dimension = reportInfo.getDimension() ;
        final String memorySize = MemorySizeFormat.formatMB(reportInfo.getPixelBytes().length) ;
        final String relativeImagePath = BitmapAnalyzer.IMAGES_DIR + File.separator + reportInfo.getImageFile().getName() ;
        String reportContent = String.format("Bitmap %d : %d x %d,  size= %s. File : %s", ++index,
                                        dimension.width, dimension.height, memorySize, relativeImagePath) ;

        writeLine("<h3>" + reportContent + "</h3>");
        int[] imageSize = calcImageSize(dimension);
        writeLine(String.format("<img src=\"%s\" width=\"%d\" height=\"%d\"></img>", relativeImagePath, imageSize[0], imageSize[1]));
        writeDivider();
    }

    private int[] calcImageSize(Dimension dimension) {
        if ( dimension.width > 1000 || dimension.height > 1000 ) {
            return new int[]{ dimension.width / 2, dimension.height / 2 } ;
        }
        return new int[]{dimension.width, dimension.height};
    }
}
