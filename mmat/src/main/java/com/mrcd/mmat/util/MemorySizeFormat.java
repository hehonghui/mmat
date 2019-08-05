package com.mrcd.mmat.util;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

public class MemorySizeFormat {

    public static final float KB = 1024 * 1.0f ;

    private MemorySizeFormat() {
    }

    public static String format(ISnapshot snapshot, int objId) {
        IObject object = null;
        try {
            object = snapshot.getObject(objId);
            String address = Long.toHexString(snapshot.mapIdToAddress(objId));

            return String.format("%s has leaked! Retained Heap Size: %s, Object : 0x%s", object.getClazz().getName(),
                    format(object.getRetainedHeapSize()), address) ;
        } catch (SnapshotException e) {
            e.printStackTrace();
        }
        return "instance id : " + object;
    }

    /**
     * 格式化字节数为特定格式
     * @param byteSize
     * @return
     */
    public static String format(long byteSize) {
        if ( byteSize >= KB * KB ) {
            return formatMB(byteSize);
        }
        return String.format("%d bytes, %.2f KB, %.2f MB ", byteSize, byteSize / KB, byteSize / KB / KB) ;
    }

    /**
     * 格式化字节数为MB
     * @param byteSize
     * @return
     */
    public static String formatMB(long byteSize) {
        return String.format("%.2f MB", byteSize / KB / KB) ;
    }
}
