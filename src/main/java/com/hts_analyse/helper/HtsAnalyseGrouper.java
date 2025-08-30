package com.hts_analyse.helper;

import com.hts_analyse.model.dto.DayGroup;
import com.hts_analyse.model.dto.GroupedResult;
import com.hts_analyse.model.dto.HtsAnalyseDto;
import com.hts_analyse.model.record.GroupKey;
import com.hts_analyse.model.record.Pair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class HtsAnalyseGrouper {

    private HtsAnalyseGrouper() {
    }

    public static List<GroupedResult> groupByStationsAndDay(List<HtsAnalyseDto> items) {
        if (items == null || items.isEmpty()) return List.of();

        Map<GroupKey, List<HtsAnalyseDto>> byGroup = items.stream()
                .collect(Collectors.groupingBy(dto -> new GroupKey(
                        safe(dto.getBaseStationId()),
                        safe(dto.getOtherStationId()),
                        safe(dto.getBaseGsmNumber()),
                        safe(dto.getOtherGsmNumber())
                )));

        return byGroup.entrySet().stream()
                .map(e -> buildGroup(e.getKey(), e.getValue()))
                .sorted(Comparator
                        .comparingInt(GroupedResult::getTotalPairs)
                        .reversed())
                .toList();
    }

    private static GroupedResult buildGroup(GroupKey key, List<HtsAnalyseDto> groupItems) {
        DoubleSummaryStatistics stats = groupItems.stream()
                .mapToDouble(dto -> dto.getDistance() != null ? dto.getDistance() : 0.0)
                .summaryStatistics();

        int totalPairs = groupItems.size();
        double avgDistance = round2(stats.getAverage());

        String groupBaseAddress = mostCommonString(groupItems.stream()
                .map(HtsAnalyseDto::getBaseGsmAddress).map(HtsAnalyseGrouper::safe).toList());
        String groupOtherAddress = mostCommonString(groupItems.stream()
                .map(HtsAnalyseDto::getOtherGsmAddress).map(HtsAnalyseGrouper::safe).toList());

        Map<LocalDate, List<HtsAnalyseDto>> byDay = groupItems.stream()
                .collect(Collectors.groupingBy(dto -> toDate(dto.getBaseGsmDateTime()),
                        TreeMap::new, Collectors.toList()));

        List<DayGroup> dayGroups = byDay.entrySet().stream()
                .map(e -> buildDayGroup(e.getKey(), e.getValue()))
                .toList();

        return GroupedResult.builder()
                .baseGsmNumber(key.baseGsmNumber())
                .otherGsmNumber(key.otherGsmNumber())
                .baseStationId(key.baseStationId())
                .otherStationId(key.otherStationId())
                .baseAddress(groupBaseAddress)
                .otherAddress(groupOtherAddress)
                .totalPairs(totalPairs)
                .distanceMeters(avgDistance)
                .byDay(dayGroups)
                .build();
    }

    private static DayGroup buildDayGroup(LocalDate date, List<HtsAnalyseDto> dayItems) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        List<Pair> pairs = dayItems.stream()
                .sorted(Comparator.comparing(HtsAnalyseDto::getBaseGsmDateTime))
                .map(dto -> new Pair(
                        dto.getBaseGsmDateTime().toLocalTime().format(timeFmt),
                        dto.getOtherGsmDateTime() != null
                                ? dto.getOtherGsmDateTime().toLocalTime().format(timeFmt)
                                : ""
                ))
                .toList();

        return DayGroup.builder()
                .date(date.toString())
                .pairs(pairs)
                .count(pairs.size())
                .build();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static LocalDate toDate(LocalDateTime dt) {
        return dt != null ? dt.toLocalDate() : LocalDate.MIN;
    }

    private static double round2(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return Math.round(v * 100.0) / 100.0;
    }

    private static String mostCommonString(List<String> values) {
        Map<String, Long> freq = values.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return freq.isEmpty()
                ? values.stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse("")
                : freq.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey).orElse("");
    }

}
