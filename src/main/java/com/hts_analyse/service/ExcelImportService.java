package com.hts_analyse.service;

import com.hts_analyse.cache.BaseStationInfoCache;
import com.hts_analyse.entity.BaseStationInfoEntity;
import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.model.dto.BaseStationDto;
import com.hts_analyse.model.dto.ExcelRecord;
import com.hts_analyse.model.dto.GeocodingResult;
import com.hts_analyse.model.record.BaseStationCandidate;
import com.hts_analyse.repository.BaseStationInfoRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
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
    private final BaseStationInfoCache baseStationInfoCache;
    private final BaseStationInfoRepository baseStationInfoRepository;
    private final HtsBatchWriter htsBatchWriter;
    private final HtsRecordBuildHelper htsRecordBuildHelper;

    @Value("${hts.analyse.interval-minutes}")
    private int intervalMinutes;

    public void importExcel(String filePath, boolean shouldAnalyseHts, boolean shouldFilterHtsRecords) {
        List<ExcelRecord> excelRecords = excelReaderService.readFullExcel(filePath);
        List<ExcelRecord> filteredExcelRecords =
                shouldFilterHtsRecords ? filterUniquePerNDuration(excelRecords) : excelRecords;

        AtomicInteger geoLocationApiUsage = new AtomicInteger();

        if (shouldAnalyseHts) {
            Map<String, String> addressKeyToAddress = collectGeocodeAddressCandidates(filteredExcelRecords);
            Map<String, GeocodingResult> geoByAddressKey = runGeocodingInParallel(addressKeyToAddress, geoLocationApiUsage);
            persistBaseStationInfosSingleThread(filteredExcelRecords, geoByAddressKey);
        }

        persistHtsRecordsSingleThread(filteredExcelRecords, shouldAnalyseHts);

        log.info("geoLocationApiUsage : {}", geoLocationApiUsage.get());
    }

    private Map<String, String> collectGeocodeAddressCandidates(List<ExcelRecord> filteredExcelRecords) {
        Map<String, String> addressKeyToAddress = new LinkedHashMap<>();

        for (ExcelRecord excelRecord : filteredExcelRecords) {
            BaseStationCandidate baseStationCandidate = toBaseStationCandidate(excelRecord);
            if (baseStationCandidate == null) continue;

            if (baseStationCandidate.hasCoordinates()) continue;

            if (baseStationInfoCache.contains(baseStationCandidate.baseStationId())) continue;

            addressKeyToAddress.putIfAbsent(baseStationCandidate.addressKey(), baseStationCandidate.address());
        }

        return addressKeyToAddress;
    }

    private Map<String, GeocodingResult> runGeocodingInParallel(Map<String, String> addressKeyToAddress, AtomicInteger geoLocationApiUsage) {
        if (addressKeyToAddress.isEmpty()) return Map.of();

        Semaphore semaphore = new Semaphore(20);
        Map<String, GeocodingResult> geoByAddressKey = new ConcurrentHashMap<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Void>> tasks = addressKeyToAddress.entrySet().stream()
                    .map(entry -> (Callable<Void>) () -> {
                        semaphore.acquire();
                        try {
                            GeocodingResult geocodingResult = geocodingService.geocode(entry.getValue());
                            if (geocodingResult != null) {
                                geoByAddressKey.put(entry.getKey(), geocodingResult);
                                geoLocationApiUsage.incrementAndGet();
                            }
                            return null;
                        } catch (Exception e) {
                            log.warn("Geocode failed. address={}", entry.getValue(), e);
                            return null;
                        } finally {
                            semaphore.release();
                        }
                    })
                    .toList();

            executor.invokeAll(tasks);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Geocoding interrupted", ie);
        }

        return geoByAddressKey;
    }

    private void persistBaseStationInfosSingleThread(
            List<ExcelRecord> filteredExcelRecords,
            Map<String, GeocodingResult> geoByAddressKey
    ) {
        List<BaseStationInfoEntity> baseStationInfoEntities = new ArrayList<>();
        Set<String> processedBaseStationIds = new HashSet<>();

        for (ExcelRecord excelRecord : filteredExcelRecords) {
            BaseStationCandidate baseStationCandidate = toBaseStationCandidate(excelRecord);
            if (baseStationCandidate == null) continue;

            if (shouldSkipBaseStationCandidate(baseStationCandidate, processedBaseStationIds)) continue;

            GeocodingResult geocodingResult = geoByAddressKey.get(baseStationCandidate.addressKey());
            if (geocodingResult == null) continue;

            BaseStationInfoEntity baseStationInfoEntity =
                    buildBaseStationInfoEntity(baseStationCandidate.baseStationId(), baseStationCandidate.address(), geocodingResult);

            baseStationInfoEntities.add(baseStationInfoEntity);
        }

        if (baseStationInfoEntities.isEmpty()) return;

        baseStationInfoRepository.saveAll(baseStationInfoEntities);
        updateBaseStationCache(baseStationInfoEntities);
    }

    private boolean shouldSkipBaseStationCandidate(BaseStationCandidate baseStationCandidate, Set<String> processedBaseStationIds) {
        if (baseStationInfoCache.contains(baseStationCandidate.baseStationId())) return true;
        return !processedBaseStationIds.add(baseStationCandidate.baseStationId());
    }

    private BaseStationInfoEntity buildBaseStationInfoEntity(
            String baseStationId,
            String address,
            GeocodingResult geocodingResult
    ) {
        return BaseStationInfoEntity.builder()
                .baseStationId(baseStationId)
                .address(address)
                .city(geocodingResult.getCity())
                .district(geocodingResult.getDistrict())
                .latitude(geocodingResult.getLatitude())
                .longitude(geocodingResult.getLongitude())
                .build();
    }

    private void updateBaseStationCache(List<BaseStationInfoEntity> baseStationInfoEntities) {
        Map<String, BaseStationInfoEntity> baseStationMap = baseStationInfoCache.getBaseStationMap();
        for (BaseStationInfoEntity baseStationInfoEntity : baseStationInfoEntities) {
            baseStationMap.put(baseStationInfoEntity.getBaseStationId(), baseStationInfoEntity);
        }
    }

    private void persistHtsRecordsSingleThread(List<ExcelRecord> filteredExcelRecords, boolean shouldAnalyseHts) {
        int batchSize = 200;
        List<HtsRecordEntity> htsRecordEntities = new ArrayList<>(batchSize);

        for (ExcelRecord excelRecord : filteredExcelRecords) {
            try {
                HtsRecordEntity htsRecordEntity = htsRecordBuildHelper.buildHtsRecordEntity(excelRecord, shouldAnalyseHts);
                if (htsRecordEntity == null) continue;

                htsRecordEntities.add(htsRecordEntity);

                if (htsRecordEntities.size() >= batchSize) {
                    persistHtsBatch(htsRecordEntities);
                }
            } catch (Exception e) {
                log.info("Kayıt işlenirken hata oluştu. ExcelRecord : {}", excelRecord, e);
            }
        }

        if (!htsRecordEntities.isEmpty()) {
            persistHtsBatch(htsRecordEntities);
        }
    }

    private void persistHtsBatch(List<HtsRecordEntity> htsRecordEntities) {
        htsBatchWriter.saveBatch(htsRecordEntities);
        htsRecordEntities.clear();
    }

    private BaseStationCandidate toBaseStationCandidate(ExcelRecord excelRecord) {
        if (excelRecord == null) return null;

        BaseStationDto dto = excelRecord.getBaseStation();
        if (dto == null) return null;

        if (StringUtils.isBlank(dto.getBaseStationId())
                || StringUtils.isBlank(dto.getAddress())) {
            return null;
        }

        String addressKey = normalizeAddress(dto.getAddress());
        if (StringUtils.isBlank(addressKey)) return null;

        return new BaseStationCandidate(
                dto.getBaseStationId(),
                dto.getAddress(),
                addressKey,
                dto.getLatitude(),
                dto.getLongitude()
        );
    }

    private static String normalizeAddress(String address) {
        if (StringUtils.isBlank(address)) return "";
        return address.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private List<ExcelRecord> filterUniquePerNDuration(List<ExcelRecord> records) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        Map<String, ExcelRecord> uniqueByInterval = new LinkedHashMap<>();

        for (ExcelRecord excelRecord : records) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(excelRecord.getRecordTime(), inputFormatter);

                int roundedMinute = (dateTime.getMinute() / intervalMinutes) * intervalMinutes;
                LocalDateTime roundedTime = dateTime
                        .withMinute(roundedMinute)
                        .withSecond(0)
                        .withNano(0);

                String baseStationId = "";
                BaseStationDto baseStationDto = excelRecord.getBaseStation();
                if (baseStationDto != null && StringUtils.isNotBlank(baseStationDto.getBaseStationId())) {
                    baseStationId = baseStationDto.getBaseStationId();
                }

                String key = roundedTime + "_" + baseStationId;
                uniqueByInterval.putIfAbsent(key, excelRecord);
            } catch (Exception e) {
                log.warn("Geçersiz tarih formatı: {}", excelRecord.getRecordTime());
            }
        }

        return List.copyOf(uniqueByInterval.values());
    }
}
