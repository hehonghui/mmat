package com.mrcd.mmat.plugin

public class TextUtils {
    private TextUtils() {
    }

    static boolean isEmpty(String text) {
        return text == null || text.trim().length() == 0
    }

    static boolean isNotEmpty(String text) {
        return !isEmpty(text)
    }
}