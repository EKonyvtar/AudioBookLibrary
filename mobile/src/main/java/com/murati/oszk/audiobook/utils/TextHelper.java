package com.murati.oszk.audiobook.utils;

public class TextHelper {
    public static String Capitalize(String input) {
        if (input == null) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
