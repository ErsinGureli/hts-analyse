package com.hts_analyse.util;

public final class GsmValidator {

    private GsmValidator(){}

    public static boolean isValid(String gsm) {
        if (gsm == null || gsm.length() < 10) return false;
        if (gsm.matches("^[a-zA-Z]+$")) return false;
        return gsm.matches("^\\+?\\d+$");
    }
}
