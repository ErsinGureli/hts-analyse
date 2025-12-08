package com.hts_analyse.helper;

import com.hts_analyse.model.dto.GroupedResult;
import com.hts_analyse.model.dto.HtsAnalyseDto;
import com.hts_analyse.model.dto.DayGroup;
import com.hts_analyse.model.record.GroupKey;
import com.hts_analyse.model.record.PairGroup;
import java.time.Duration;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class HtsAnalyseGrouper {

    public List<GroupedResult> groupByStationsAndDay(List<HtsAnalyseDto> items) {
        return items.stream()
                .collect(Collectors.groupingBy(GroupKey::from))
                .values()
                .stream()
                .map(HtsAnalyseGrouper::buildGroupedResult)
                .toList();
    }

    private static GroupedResult buildGroupedResult(List<HtsAnalyseDto> groupItems) {
        HtsAnalyseDto first = groupItems.getFirst();

        Map<LocalDate, List<HtsAnalyseDto>> groupedByDay = groupItems.stream()
                .collect(Collectors.groupingBy(dto -> dto.getBaseGsmDateTime().toLocalDate()));

        List<DayGroup> byDay = groupedByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> buildDayGroup(entry.getKey(), entry.getValue()))
                .toList();

        Set<String> baseStationIds = groupItems.stream()
                .map(HtsAnalyseDto::getBaseStationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> otherStationIds = groupItems.stream()
                .map(HtsAnalyseDto::getOtherStationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        double avgDistance = groupItems.stream()
                .map(HtsAnalyseDto::getDistance)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return GroupedResult.builder()
                .baseGsmNumber(first.getBaseGsmNumber())
                .otherGsmNumber(first.getOtherGsmNumber())
                .baseAddress(first.getBaseGsmAddress())
                .otherAddress(first.getOtherGsmAddress())
                .baseLatitude(first.getBaseLatitude())
                .baseLongitude(first.getBaseLongitude())
                .otherLatitude(first.getOtherLatitude())
                .otherLongitude(first.getOtherLongitude())
                .baseStationIds(baseStationIds)
                .otherStationIds(otherStationIds)
                .totalPairs(groupItems.size())
                .distanceMeters(avgDistance)
                .byDay(byDay)
                .build();
    }

    private static DayGroup buildDayGroup(LocalDate date, List<HtsAnalyseDto> dayItems) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        Map<LocalDateTime, List<LocalDateTime>> baseToOthersMap = new HashMap<>();

        for (HtsAnalyseDto dto : dayItems) {
            LocalDateTime baseTime = dto.getBaseGsmDateTime();
            LocalDateTime otherTime = dto.getOtherGsmDateTime();
            if (baseTime == null || otherTime == null) continue;

            baseToOthersMap
                    .computeIfAbsent(baseTime, k -> new ArrayList<>())
                    .add(otherTime);
        }

        List<PairGroup> pairGroups = baseToOthersMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // baseTime'a göre sıralı
                .map(entry -> {
                    LocalDateTime base = entry.getKey();
                    List<LocalDateTime> others = entry.getValue();

                    // her baseTime için en yakın otherTime
                    LocalDateTime closest = others.stream()
                            .min(Comparator.comparingLong(o ->
                                    Math.abs(Duration.between(base, o).toMillis())))
                            .orElse(null);

                    return new AbstractMap.SimpleEntry<>(base, closest);
                })
                // 🔹 Buradan itibaren: aynı otherTime’a sahip olanlardan en yakını seç
                .collect(Collectors.toMap(
                        Map.Entry::getValue, // key = otherTime
                        Map.Entry::getKey,   // value = baseTime
                        (base1, base2) -> {  // merge function → daha yakın baseTime’ı seç
                            long diff1 = Math.abs(Duration.between(base1, base2).toMillis());
                            return diff1 <= 0 ? base1 : base2;
                        }
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) // baseTime sırasına göre gösterelim
                .map(entry -> new PairGroup(
                        entry.getValue().toLocalTime().format(timeFmt), // baseTime
                        entry.getKey() != null ? entry.getKey().toLocalTime().format(timeFmt) : null // otherTime
                ))
                .toList();

        return DayGroup.builder()
                .date(date.toString())
                .pairGroups(pairGroups)
                .count(pairGroups.size())
                .build();
    }
}
