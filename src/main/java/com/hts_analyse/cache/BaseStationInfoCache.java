package com.hts_analyse.cache;

import com.hts_analyse.entity.BaseStationInfoEntity;
import com.hts_analyse.repository.BaseStationInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaseStationInfoCache {

    private final BaseStationInfoRepository baseStationInfoRepository;

    @Getter
    private Map<String, BaseStationInfoEntity> baseStationMap;

    @PostConstruct
    public void init() {
        log.info("Loading base_station_info cache...");
        baseStationMap = baseStationInfoRepository.findAll().stream()
                .collect(Collectors.toMap(
                        BaseStationInfoEntity::getBaseStationId,
                        e -> e
                ));
        log.info("Base station cache loaded: {} entries", baseStationMap.size());
    }

    public BaseStationInfoEntity get(String baseStationId) {
        return baseStationMap.get(baseStationId);
    }

    public boolean contains(String baseStationId) {
        return baseStationMap.containsKey(baseStationId);
    }
}