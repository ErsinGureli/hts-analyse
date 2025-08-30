package com.hts_analyse.service;

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

}
