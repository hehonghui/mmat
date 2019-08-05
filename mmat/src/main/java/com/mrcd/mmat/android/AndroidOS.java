package com.mrcd.mmat.android;

import com.mrcd.mmat.util.CollectionsUtil;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.parser.model.PrimitiveArrayImpl;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.Field;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.ObjectReference;

import java.util.Collection;

public class AndroidOS {
    /**
     * android sdk 的版本
     */
    public static int SDK_INT = 0 ;
    /**
     * 系统版本文字描述
     */
    public static String RELEASE = "";
    /**
     * 系统厂商
     */
    public static String MANUFACTURER = "";
    /**
     * 设备名字
     */
    public static String MODEL = "";

    private AndroidOS() {
    }

    public static void parse(ISnapshot snapshot) {
        System.out.println();
        System.out.println("====== Android Device Info ======");
        parseAndroidBrand(snapshot);
        parseAndroidOsVersion(snapshot);
    }

    private static void parseAndroidOsVersion(ISnapshot snapshot) {
        try {
            Collection<IClass> classes = snapshot.getClassesByName("android.os.Build$VERSION", false);
            if (CollectionsUtil.isEmpty(classes)) {
                return;
            }
            assert classes.size() == 1;
            final IClass clazz = classes.iterator().next();
            for (Field staticField: clazz.getStaticFields()) {
                if ( staticField != null && "SDK_INT".equalsIgnoreCase(staticField.getName()) ) {
                    AndroidOS.SDK_INT = (int)staticField.getValue() ;
                    System.out.println("Android SDK API : " + AndroidOS.SDK_INT );
                }

                if ( staticField.getName().equalsIgnoreCase("RELEASE") ) {
                    // 读取 Build.VERSION 中的静态字段中的字符串值
                    AndroidOS.RELEASE = resolveStaticString(staticField) ;
                    System.out.println("Android RELEASE version : " + AndroidOS.RELEASE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            AndroidOS.SDK_INT = AndroidVersions.P ;
        }
    }


    private static void parseAndroidBrand(ISnapshot snapshot) {
        try {
            Collection<IClass> classes = snapshot.getClassesByName("android.os.Build", false);
            if (CollectionsUtil.isEmpty(classes)) {
                return;
            }
            assert classes.size() == 1;
            final IClass clazz = classes.iterator().next();
            for (Field staticField: clazz.getStaticFields()) {
                if ( staticField.getName().equalsIgnoreCase("MANUFACTURER") ) {
                    AndroidOS.MANUFACTURER = resolveStaticString(staticField) ;
                    System.out.println("Android MANUFACTURER Name : " + AndroidOS.MANUFACTURER);
                }
                if ( staticField.getName().equalsIgnoreCase("MODEL") ) {
                    AndroidOS.MODEL = resolveStaticString(staticField) ;
                    System.out.println("Android device : " + AndroidOS.MODEL);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String resolveStaticString(Field staticField) throws SnapshotException {
        try {
            IObject realObj = ((ObjectReference)staticField.getValue()).getObject();
            PrimitiveArrayImpl primitiveArray = (PrimitiveArrayImpl)realObj.resolveValue("value") ;
            final Object value = primitiveArray.getValueArray() ;
            if ( value == null ) {
                return "";
            } else if ( value instanceof byte[] ) {
                return new String((byte[])value) ;
            } else if ( value instanceof char[] ) {
                return new String((char[])value) ;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
