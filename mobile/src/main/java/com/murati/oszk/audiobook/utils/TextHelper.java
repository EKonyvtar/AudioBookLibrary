package com.murati.oszk.audiobook.utils;

/**
 * Created by akosmurati on 20/10/17.
 */

public class TextHelper {
    public static String Capitalize(String input) {
        if (input == null) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
