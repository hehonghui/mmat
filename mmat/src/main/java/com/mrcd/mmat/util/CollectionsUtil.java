package com.mrcd.mmat.util;

import java.util.Collection;

public class CollectionsUtil {

    private CollectionsUtil() {
    }

    public static boolean isEmpty(Collection<?> collections) {
        return collections == null || collections.size() == 0 ;
    }
}
