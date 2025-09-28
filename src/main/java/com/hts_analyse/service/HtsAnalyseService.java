package com.hts_analyse.service;

import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.helper.HtsAnalyseGrouper;
import com.hts_analyse.mapper.HtsAnalyseMapper;
import com.hts_analyse.mapper.HtsRecordMapper;
import com.hts_analyse.model.dto.CommonContactDto;
import com.hts_analyse.model.dto.CommonContactShortDto;
import com.hts_analyse.model.dto.GeocodingResult;
import com.hts_analyse.model.dto.GroupedResult;
import com.hts_analyse.model.dto.HtsAnalyseDto;
import com.hts_analyse.model.dto.HtsRecordDto;
import com.hts_analyse.model.response.CommonContactResponse;
import com.hts_analyse.util.GeoUtils;
import com.hts_analyse.util.GsmValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HtsAnalyseService {

    private final GeocodingService geocodingService;
    private final HtsRecordQueryService htsRecordQueryService;

    public List<GroupedResult> analyseDistance(String baseGsmNumber, List<String> compareGsmNumbers, int minute, int distance, LocalDate startDate, LocalDate endDate) {
        List<HtsRecordEntity> recordEntities = findHtsRecordsByGsmNumber(baseGsmNumber, startDate, endDate);

        Map<Long, List<HtsRecordEntity>> recordsMap = new HashMap<>();

        for(HtsRecordEntity htsRecordEntity : recordEntities){
            List<HtsRecordEntity> otherRecordEntities = findHtsRecordsByGsmNumberAndRecordTimeAndDistance(compareGsmNumbers,
                    htsRecordEntity.getRecordDatetime(), minute, htsRecordEntity.getLatitude(), htsRecordEntity.getLongitude(),distance);
            if(!otherRecordEntities.isEmpty()) {
                recordsMap.put(htsRecordEntity.getId(), otherRecordEntities);
            }
        }
        List<HtsAnalyseDto> htsAnalyseDtoList = cretaeHtsAnalyseDtoList(recordsMap, recordEntities, distance);
        return HtsAnalyseGrouper.groupByStationsAndDay(htsAnalyseDtoList);
    }

    public List<HtsRecordDto> findNearbyBazRecords(String address, List<String> gsmNumbers, int distance,
            LocalDateTime startTime, LocalDateTime endTime) {
        GeocodingResult g = geocodingService.geocode(address);
        if (g == null) return Collections.emptyList();

        double lat = g.getLatitude();
        double lon = g.getLongitude();

        return HtsRecordMapper.toDtoList(htsRecordQueryService.findNearbyRecords(gsmNumbers, startTime, endTime, lat, lon, distance));
    }

    public CommonContactResponse findCommonContacts(String gsm1, String gsm2) {
        List<HtsRecordEntity> records1 = htsRecordQueryService.findAllByGsmNumber(gsm1);
        List<HtsRecordEntity> records2 = htsRecordQueryService.findAllByGsmNumber(gsm2);

        Map<String, List<HtsRecordEntity>> map1 = groupByOtherNumber(records1);
        Map<String, List<HtsRecordEntity>> map2 = groupByOtherNumber(records2);

        Set<String> commonGsms = getCommonKeys(map1, map2);

        List<CommonContactDto> details = new ArrayList<>();
        List<CommonContactShortDto> shortList = new ArrayList<>();

        for (String commonGsm : commonGsms) {
            if (!GsmValidator.isValid(commonGsm)) {
                continue;
            }
            List<HtsRecordEntity> gsm1Records = map1.getOrDefault(commonGsm, List.of());
            List<HtsRecordEntity> gsm2Records = map2.getOrDefault(commonGsm, List.of());

            CommonContactDto contactDto = HtsAnalyseMapper.buildDetailedDto(gsm1, gsm2, commonGsm, gsm1Records, gsm2Records);
            details.add(contactDto);

            Optional<CommonContactShortDto> shortDtoOpt = HtsAnalyseMapper.buildShortDto(commonGsm, gsm1Records, gsm2Records);
            shortDtoOpt.ifPresent(shortList::add);
        }

        return CommonContactResponse.builder()
                .totalCommonCount(commonGsms.size())
                .commonCommunications(shortList)
                .commonContacts(details)
                .build();
    }

    private Map<String, List<HtsRecordEntity>> groupByOtherNumber(List<HtsRecordEntity> records) {
        return records.stream()
                .filter(r -> r.getOtherNumber() != null)
                .collect(Collectors.groupingBy(HtsRecordEntity::getOtherNumber));
    }

    private Set<String> getCommonKeys(Map<String, List<HtsRecordEntity>> map1, Map<String, List<HtsRecordEntity>> map2) {
        Set<String> common = new HashSet<>(map1.keySet());
        common.retainAll(map2.keySet());
        return common;
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
                            .baseStationId(htsRecordEntity.getBaseStationId())
                            .otherStationId(otherHtsRecordEntity.getBaseStationId())
                            .latitude(htsRecordEntity.getLatitude())
                            .longitude(htsRecordEntity.getLongitude())
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

    private List<HtsRecordEntity> findHtsRecordsByGsmNumber(String gsmNumber, LocalDate startDate, LocalDate endDate){
        if (startDate != null && endDate != null) {
            return htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeBetween(
                    gsmNumber, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        } else if (startDate != null) {
            return htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeAfter(
                    gsmNumber, startDate.atStartOfDay());
        } else if (endDate != null) {
            return htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeBefore(
                    gsmNumber, endDate.atTime(LocalTime.MAX));
        } else {
            return htsRecordQueryService.findAllByGsmNumber(gsmNumber);
        }
    }

    private List<HtsRecordEntity> findHtsRecordsByGsmNumberAndRecordTimeAndDistance(List<String> compareGsmNumbers, LocalDateTime recordTime, int minute,
            Double latitude, Double longitude, int distance){
        LocalDateTime startTime = recordTime.minusMinutes(minute);
        LocalDateTime endTime = recordTime.plusMinutes(minute);
        return htsRecordQueryService.findNearbyRecords(compareGsmNumbers, startTime, endTime, latitude, longitude,distance);
    }

    public List<Object[]> findLastNamesWithCount(String baseGsmNumber){
        return htsRecordQueryService.findLastNamesWithCount(baseGsmNumber);
    }

    public List<Object[]> findFullNameIdentityNoWithCount(String gsmNumber, String lastName) {
        return htsRecordQueryService.findFullNameIdentityNoWithCount(gsmNumber, lastName);
    }
}
