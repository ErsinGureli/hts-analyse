package com.hts_analyse.service;

import com.hts_analyse.cache.BaseStationInfoCache;
import com.hts_analyse.entity.BaseStationInfoEntity;
import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.model.ExcelRecord;
import com.hts_analyse.model.GeocodingResult;
import com.hts_analyse.model.Location;
import com.hts_analyse.repository.BaseStationInfoRepository;
import com.hts_analyse.repository.HtsRecordRepository;
import com.hts_analyse.util.DateUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final ExcelReaderService excelReaderService;
    private final GeocodingService geocodingService;
    private final HtsRecordRepository htsRecordRepository;
    private final NominatimGeocoder nominatimGeocoder;
    private final GeoapifyGeocoderService geoapifyGeocoderService;
    private final BaseStationInfoCache baseStationInfoCache;
    private final BaseStationInfoRepository baseStationInfoRepository;

    @Value("${hts.analyse.interval-minutes}")
    private int intervalMinutes;

    public void importExcel(String filePath) {
        List<ExcelRecord> excelRecords = filterUniquePerNDuration(excelReaderService.readExcel(filePath));

        AtomicInteger geoLocationApiUsage = new AtomicInteger();

        for (ExcelRecord excelRecord : excelRecords) {
            try {
                if (StringUtils.isBlank(excelRecord.getBaseStation().getAddress())) {
                    continue;
                }

                String baseStationId = excelRecord.getBaseStation().getBaseStationId();
                BaseStationInfoEntity stationInfo = baseStationInfoCache.getBaseStationMap()
                        .computeIfAbsent(baseStationId, id -> {
                            GeocodingResult g = geocodingService.geocode(excelRecord.getBaseStation().getAddress());
                            if (g == null) {
                                return null;
                            }
                            geoLocationApiUsage.getAndIncrement();

                            BaseStationInfoEntity entity = BaseStationInfoEntity.builder()
                                    .baseStationId(id)
                                    .address(excelRecord.getBaseStation().getAddress())
                                    .city(g.getCity())
                                    .district(g.getDistrict())
                                    .latitude(g.getLatitude())
                                    .longitude(g.getLongitude())
                                    .build();

                            baseStationInfoRepository.save(entity);
                            return entity;
                        });

                if (stationInfo == null) {
                    continue;
                }

                HtsRecordEntity hts = HtsRecordEntity.builder()
                        .gsmNumber(excelRecord.getGsmNumber())
                        .recordType(excelRecord.getRecordType())
                        .otherNumber(excelRecord.getOtherNumber())
                        .recordDatetime(DateUtil.convertStringToLocalDateTime(excelRecord.getRecordTime()))
                        .fullName(excelRecord.getFullName())
                        .baseStationId(baseStationId)
                        .operator(excelRecord.getBaseStation().getOperator())
                        .address(stationInfo.getAddress())
                        .city(stationInfo.getCity())
                        .district(stationInfo.getDistrict())
                        .latitude(stationInfo.getLatitude())
                        .longitude(stationInfo.getLongitude())
                        .build();

                htsRecordRepository.save(hts);
            } catch (Exception e) {
                log.warn("Kayıt işlenirken hata oluştu. ExcelRecord : {}", excelRecord, e);
            }
        }
        log.info("geoLocationApiUsage : {}", geoLocationApiUsage.get());
    }

    private List<ExcelRecord> filterUniquePerNDuration(List<ExcelRecord> records) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        Map<String, ExcelRecord> uniqueByInterval = new LinkedHashMap<>();

        for (ExcelRecord record : records) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(record.getRecordTime(), inputFormatter);

                int roundedMinute = (dateTime.getMinute() / intervalMinutes) * intervalMinutes;
                LocalDateTime roundedTime = dateTime
                        .withMinute(roundedMinute)
                        .withSecond(0)
                        .withNano(0);

                String key = roundedTime.toString(); // örneğin: 2024-06-12T14:35

                uniqueByInterval.putIfAbsent(key, record);

            } catch (Exception e) {
                log.warn("Geçersiz tarih formatı: {}", record.getRecordTime());
            }
        }

        return List.copyOf(uniqueByInterval.values());
    }

    public void importExcelTests(String filePath) {
        List<ExcelRecord> excelRecords = excelReaderService.readExcel(filePath);
        for (ExcelRecord excelRecord : excelRecords) {
            try {
                if(StringUtils.isBlank(excelRecord.getBaseStation().getAddress())) {
                    continue;
                }

                Location location = nominatimGeocoder.geocode(excelRecord.getBaseStation().getAddress());
                if(location != null && StringUtils.isNotBlank(location.getCity()) && StringUtils.isNotBlank(location.getState())) {

                    HtsRecordEntity htsRecordEntity = HtsRecordEntity.builder()
                            .gsmNumber(excelRecord.getGsmNumber())
                            .recordType(excelRecord.getRecordType())
                            .otherNumber(excelRecord.getOtherNumber())
                            .recordDatetime(DateUtil.convertStringToLocalDateTime(excelRecord.getRecordTime()))
                            .fullName(excelRecord.getFullName())
                            .baseStationId(excelRecord.getBaseStation().getBaseStationId())
                            .operator(excelRecord.getBaseStation().getOperator())
                            .address(excelRecord.getBaseStation().getAddress())
                            .city(location.getCity())
                            .district(location.getState())
                            .latitude(location.getLat())
                            .longitude(location.getLon())
                            .build();
                    htsRecordRepository.save(htsRecordEntity);
                } 

            } catch (Exception e) {
                log.warn("Kayıt işlenirken hata oluştu: {}", excelRecord, e);
            }
        }
    }

    public String checkFreeApi(String address){
        try {
            Location location = nominatimGeocoder.geocode(address);
            if(location != null && StringUtils.isNotBlank(location.getCity()) && StringUtils.isNotBlank(location.getState())) {
                return "city: " + location.getCity() + "--  district: " +location.getState() + "--- latitude: " + location.getLat()
                        + "-----   longitude: " + location.getLon();
            }
            return "Record not found";
        } catch (Exception e) {
            log.warn("Kayıt işlenirken hata oluştu: {}", address, e);
            return "Kayıt işlenirken hata oluştu: {}";
        }
    }

    public String checkByGeoapify(String address){
        try {
            Location location = geoapifyGeocoderService.geocode(address);
            if(location != null && StringUtils.isNotBlank(location.getCity()) && StringUtils.isNotBlank(location.getState())) {
                return "city: " + location.getCity() + "--  district: " +location.getState() + "--- latitude: " + location.getLat()
                        + "-----   longitude: " + location.getLon();
            }
            return "Record not found";
        } catch (Exception e) {
            log.warn("Kayıt işlenirken hata oluştu: {}", address, e);
            return "Kayıt işlenirken hata oluştu: {}";
        }
    }
    
}
