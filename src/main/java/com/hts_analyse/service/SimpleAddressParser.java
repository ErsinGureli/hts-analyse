package com.hts_analyse.service;

import static com.hts_analyse.util.TurkishCharacterConverter.turkishToAscii;

import com.hts_analyse.model.dto.CityDistrict;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SimpleAddressParser {

    public CityDistrict parseAdres(String adres, Map<String, Set<String>> cityDistrictMap) {
        String adresUpper = turkishToAscii(adres.toUpperCase(Locale.ROOT).trim());

        // 1. Adresi virgüle göre parçala
        String[] parts = adresUpper.split(",");

        String il = null;
        String ilce = null;

        // 2. En sondaki parça genelde il adıdır
        if (parts.length > 1) {
            String afterComma = parts[parts.length - 1].trim();
            if (cityDistrictMap.containsKey(afterComma)) {
                il = afterComma;

                // 3. Öncesine bakarak ilçe arayalım
                String beforeComma = parts[parts.length - 2];

                // İlin ilçeleri içinde geçen var mı?
                for (String ilceCandidate : cityDistrictMap.get(il)) {
                    if (beforeComma.contains(ilceCandidate)) {
                        ilce = ilceCandidate;
                        break;
                    }
                }
            }
        }

        // 4. Hala il bulunamadıysa tüm adres içinde ara
        if (il == null) {
            for (String ilKey : cityDistrictMap.keySet()) {
                if (adresUpper.contains(ilKey)) {
                    il = ilKey;
                    break;
                }
            }
        }

        // 5. İl bulunduysa ilçeyi tekrar ara
        if (il != null && ilce == null) {
            for (String ilceCandidate : cityDistrictMap.get(il)) {
                if (adresUpper.contains(ilceCandidate)) {
                    ilce = ilceCandidate;
                    break;
                }
            }
        }

        return new CityDistrict(il, ilce);
    }
}
