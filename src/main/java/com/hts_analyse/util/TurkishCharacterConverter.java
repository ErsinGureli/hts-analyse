package com.hts_analyse.util;

public class TurkishCharacterConverter {
    public static String turkishToAscii(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace('ç', 'c')
                .replace('Ç', 'C')
                .replace('ğ', 'g')
                .replace('Ğ', 'G')
                .replace('ı', 'i')
                .replace('İ', 'I')
                .replace('ö', 'o')
                .replace('Ö', 'O')
                .replace('ş', 's')
                .replace('Ş', 'S')
                .replace('ü', 'u')
                .replace('Ü', 'U');
    }

}