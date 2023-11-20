package com.anilrpinto.filemanager.utils;

public class Utility {

    public static String truncate(String text, int length) {

        if (text.length() <= length)
            return text;

        int diff = (text.length() - length)/2;
        int mid = text.length()/2;

        return text.substring(0, mid-diff) + "..." + text.substring(mid+diff);
    }

}
