package com.hts_analyse.service;

import com.hts_analyse.model.dto.ImeiSharedGsmDto;
import com.hts_analyse.model.response.ImeiSharedGsmResponse;
import com.hts_analyse.repository.GsmImeiRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImeiAnalyseService {

    private final HtsRecordQueryService htsRecordQueryService;
    private final GsmImeiRepository gsmImeiRepository;

    public ImeiSharedGsmResponse findSharedImeis(
            List<String> gsmNumbers,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        List<String> normalizedGsms = gsmNumbers == null ? List.of() : gsmNumbers.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        if (normalizedGsms.size() < 2) {
            return ImeiSharedGsmResponse.builder()
                    .totalImeiCount(0)
                    .sharedImeis(List.of())
                    .build();
        }

        List<Object[]> rows;
        if (startTime != null && endTime != null) {
            rows = htsRecordQueryService.findDistinctGsmNumbersByImeiAndRecordTimeBetween(
                    normalizedGsms, startTime, endTime);
        } else {
            rows = htsRecordQueryService.findDistinctGsmNumbersByImei(normalizedGsms);
        }
        Map<String, Set<String>> imeiToGsms = new HashMap<>();

        for (Object[] row : rows) {
            String imei = (String) row[0];
            String gsmNumber = (String) row[1];
            if (StringUtils.isBlank(imei) || StringUtils.isBlank(gsmNumber)) {
                continue;
            }
            imeiToGsms.computeIfAbsent(imei, key -> new LinkedHashSet<>()).add(gsmNumber);
        }

        List<ImeiSharedGsmDto> sharedImeis = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : imeiToGsms.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            List<String> gsmList = new ArrayList<>(entry.getValue());
            gsmList.sort(String::compareTo);
            sharedImeis.add(ImeiSharedGsmDto.builder()
                    .imei(entry.getKey())
                    .gsmCount(gsmList.size())
                    .gsmNumbers(gsmList)
                    .build());
        }

        sharedImeis.sort(Comparator
                .comparingInt(ImeiSharedGsmDto::getGsmCount).reversed()
                .thenComparing(ImeiSharedGsmDto::getImei, Comparator.nullsLast(String::compareTo)));

        return ImeiSharedGsmResponse.builder()
                .totalImeiCount(sharedImeis.size())
                .sharedImeis(sharedImeis)
                .build();
    }

    @Transactional
    public int syncGsmImeiForGsm(String gsmNumber) {
        if (StringUtils.isBlank(gsmNumber)) {
            throw new IllegalArgumentException("gsmNumber must not be blank");
        }

        List<String> imeis = htsRecordQueryService.findDistinctImeisByGsmNumber(gsmNumber);
        int insertedCount = 0;

        for (String imei : imeis) {
            if (StringUtils.isBlank(imei) || !imei.matches("\\d{15}")) {
                continue;
            }
            insertedCount += gsmImeiRepository.insertIgnore(gsmNumber, imei);
        }

        return insertedCount;
    }
}
