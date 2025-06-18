package com.hts_analyse.service;

import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.model.HtsAnalyseDto;
import com.hts_analyse.repository.HtsRecordRepository;
import com.hts_analyse.util.GeoUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HtsAnalyseService {

    private final HtsRecordRepository htsRecordRepository;

    public List<HtsAnalyseDto> analyseDistance(String baseGsmNumber, List<String> compareGsmNumbers, int minute, int distance) {
        List<HtsRecordEntity> recordEntities = findHtsRecordsByGsmNumber(baseGsmNumber);
        Map<Long, List<HtsRecordEntity>> recordsMap = new HashMap<>();

        for(HtsRecordEntity htsRecordEntity : recordEntities){
            List<HtsRecordEntity> otherRecordEntities = findHtsRecordsByGsmNumberAndRecordTimeAndDistance(compareGsmNumbers,
                    htsRecordEntity.getRecordDatetime(), minute, htsRecordEntity.getLatitude(), htsRecordEntity.getLongitude(),distance);
            if(!otherRecordEntities.isEmpty()) {
                recordsMap.put(htsRecordEntity.getId(), otherRecordEntities);
            }
        }
        return cretaeHtsAnalyseDtoList(recordsMap, recordEntities, distance);
    }

    private List<HtsAnalyseDto> cretaeHtsAnalyseDtoList(Map<Long, List<HtsRecordEntity>> recordsMap, List<HtsRecordEntity> recordEntities, int distance) {
        List<HtsAnalyseDto> htsAnalyseDtoList = new ArrayList<>();
        for(HtsRecordEntity htsRecordEntity : recordEntities){
            List<HtsRecordEntity> otherRecordEntities = recordsMap.get(htsRecordEntity.getId());
            if(otherRecordEntities != null) {
                for (HtsRecordEntity otherHtsRecordEntity : otherRecordEntities) {
                    htsAnalyseDtoList.add(HtsAnalyseDto.builder()
                            .baseGsmNumber(htsRecordEntity.getGsmNumber())
                            .distance(GeoUtils.calculateDistance(htsRecordEntity.getLatitude(), htsRecordEntity.getLongitude(),
                                    otherHtsRecordEntity.getLatitude(), otherHtsRecordEntity.getLongitude()))
                            .baseGsmDateTime(htsRecordEntity.getRecordDatetime())
                            .otherGsmAddress(otherHtsRecordEntity.getAddress())
                            .otherGsmDateTime(otherHtsRecordEntity.getRecordDatetime())
                            .otherGsmNumber(otherHtsRecordEntity.getGsmNumber())
                            .baseGsmAddress(htsRecordEntity.getAddress())
                            .build());
                }
            }
        }

        return filterListByDistance(htsAnalyseDtoList, distance);
    }

    private List<HtsAnalyseDto> filterListByDistance(List<HtsAnalyseDto> htsAnalyseDtoList, int distance){
        Predicate<HtsAnalyseDto> isFartherThan1Km = dto -> dto.getDistance() <= distance;
        return htsAnalyseDtoList.stream()
                .filter(isFartherThan1Km)
                .toList();
    }

    private List<HtsRecordEntity> findHtsRecordsByGsmNumber(String gsmNumber){
        return htsRecordRepository.findAllByGsmNumber(gsmNumber);
    }

    private List<HtsRecordEntity> findHtsRecordsByGsmNumberAndRecordTimeAndDistance(List<String> compareGsmNumbers, LocalDateTime recordTime, int minute,
            Double latitude, Double longitude, int distance){
        LocalDateTime startTime = recordTime.minusMinutes(minute);
        LocalDateTime endTime = recordTime.plusMinutes(minute);
        return htsRecordRepository.findNearbyRecords(compareGsmNumbers, startTime, endTime, latitude, longitude,distance);
    }
}
