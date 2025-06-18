package com.hts_analyse.util;

import static com.hts_analyse.util.TurkishCharacterConverter.turkishToAscii;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CityLoader {

    public static Map<String, Set<String>> loadCityMap(String csvDosyaYolu) {
        Map<String, Set<String>> ilIlceMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvDosyaYolu))) {
            String satir;
            boolean ilkSatir = true;

            while ((satir = br.readLine()) != null) {
                if (ilkSatir) {
                    ilkSatir = false; // Başlık satırını atla
                    continue;
                }

                String[] parcalar = satir.split(",");
                if (parcalar.length < 2) {
                    continue;
                }

                String il = turkishToAscii(parcalar[0].trim().toUpperCase(Locale.ROOT));
                String ilce = turkishToAscii(parcalar[1].trim().toUpperCase(Locale.ROOT));

                ilIlceMap
                    .computeIfAbsent(il, k -> new HashSet<>())
                    .add(ilce);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ilIlceMap;
    }
}