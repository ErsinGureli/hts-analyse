package com.hts_analyse.service;

import com.hts_analyse.util.CityLoader;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@Getter
public class CityLoaderService {

    @Value("${city.csv.path}")
    private String csvDosyaYolu;

    private Map<String, Set<String>> cityMap;

    @PostConstruct
    public void init() {
        log.info("Loading city map from CSV: {}", csvDosyaYolu);
        this.cityMap = CityLoader.loadCityMap(csvDosyaYolu);
        log.info("City map loaded. Total cities: {}", cityMap.size());
    }
}
