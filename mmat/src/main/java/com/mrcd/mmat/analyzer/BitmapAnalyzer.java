package com.mrcd.mmat.analyzer;

import com.mrcd.mmat.MMATConfig;
import com.mrcd.mmat.bitmap.BitmapDecoder;
import com.mrcd.mmat.bitmap.HprofBitmapProvider;
import com.mrcd.mmat.report.BitmapReport;
import com.mrcd.mmat.report.Reportable;
import com.mrcd.mmat.util.CollectionsUtil;
import com.mrcd.mmat.util.FileUtils;
import com.mrcd.mmat.util.MemorySizeFormat;

import org.eclipse.mat.parser.model.PrimitiveArrayImpl;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.IProgressListener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * bitmap 分析, 可以将Bitmap 导出到本地路径中, 用来查看一些尺寸、大小可疑的Bitmap
 */
public class BitmapAnalyzer implements HprofAnalyzer {

    public static final String IMAGES_DIR = "images";

    private static final String[] BITMAP_CLASS = {
            "android.graphics.Bitmap"
    };

    protected final BitmapReport mHprofReport ;
    protected MMATConfig.BitmapReportConfig mBitmapReportConfig ;

    public BitmapAnalyzer(MMATConfig config, Reportable hprofReport) {
        this.mHprofReport = new BitmapReport(config.bitmapConfig, hprofReport);
        this.mBitmapReportConfig = config.bitmapConfig ;
    }

    @Override
    public void analysis(ISnapshot snapshot, IProgressListener listener) {
        // 清理之前的 图片
        FileUtils.clearDir(getDumpImagesDir());
        try {
            for (String name : BITMAP_CLASS) {
                Collection<IClass> classes = snapshot.getClassesByName(name, false);
                if (CollectionsUtil.isEmpty(classes)) {
                    continue;
                }
                assert classes.size() == 1;
                IClass clazz = classes.iterator().next();
                int[] objIds = clazz.getObjectIds();
                long minRetainedSize = snapshot.getMinRetainedSize(objIds, listener);
                System.out.println("\n=================== BITMAP INSTANCE SUMMARY ================\n");
                System.out.println(String.format("Detected %d %s instances, retained size : %s", objIds.length, clazz.getName(), MemorySizeFormat.format(minRetainedSize)));
                System.out.println("\n");
                for (int i = 0; i < objIds.length; i++) {
                    dumpBitmap(snapshot, objIds[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mHprofReport.onFinish();
        }
    }

    private void dumpBitmap(ISnapshot snapshot, int objId) {
        try {
            final IObject bmp = snapshot.getObject(objId);
            final String address = Long.toHexString(snapshot.mapIdToAddress(objId));
            int width = ((Integer) bmp.resolveValue("mWidth")).intValue();
            int height = ((Integer) bmp.resolveValue("mHeight")).intValue();
            final byte[] bmpBytes = readBitmapBytes(bmp, width, height, address);
            saveBitmapToPNG(objId, address, width, height, bmpBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void saveBitmapToPNG(int objId, String address, int width, int height,
                                 byte[] bitmapBytes) {
        if ( !mBitmapReportConfig.shouldReport(width, height) || bitmapBytes == null ) {
            return;
        }
        File outputFile = new File(getDumpImagesDir(), "bitmap_0x" + address + "_" + bitmapBytes.length + "_bytes.png");
        final HprofBitmapProvider bitmapProvider = new HprofBitmapProvider(bitmapBytes, width, height, outputFile) ;
        // decode image from bitmap bytes
        final BufferedImage image = BitmapDecoder.decode(bitmapProvider) ;
        OutputStream inb = null ;
        ImageOutputStream imageOutput = null;
        try {
            inb = new FileOutputStream(outputFile);
            final ImageWriter wrt = ImageIO.getImageWritersByFormatName("png").next();
            imageOutput = ImageIO.createImageOutputStream(inb);
            wrt.setOutput(imageOutput);
            wrt.write(image);
            System.out.println(String.format("address = 0x%s, %d x %d, size= %s", address, width, height, MemorySizeFormat.format(bitmapBytes.length)));
            mHprofReport.onReport(bitmapProvider);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeQuietly(inb);
            FileUtils.closeQuietly(imageOutput);
        }
    }

    private static byte[] readBitmapBytes(IObject bmp, int width, int height, String address) throws Exception {
        if ((height <= 0) || (width <= 0)) {
            System.out.println(String.format("Bitmap address=%s has bad height %d or width %d!", address, height, width));
            return null;
        }

        final PrimitiveArrayImpl bitmapBuffer = (PrimitiveArrayImpl) bmp.resolveValue("mBuffer");
        if (bitmapBuffer == null) {
            return null;
        }
        return (byte[]) bitmapBuffer.getValueArray();
    }

    private static File getDumpImagesDir() {
        File file = new File(FileUtils.getReportDir(), File.separator + IMAGES_DIR + File.separator);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}
