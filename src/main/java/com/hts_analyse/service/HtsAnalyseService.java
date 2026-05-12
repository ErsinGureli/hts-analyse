package com.hts_analyse.service;

import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.helper.HtsAnalyseGrouper;
import com.hts_analyse.mapper.HtsAnalyseMapper;
import com.hts_analyse.mapper.HtsRecordMapper;
import com.hts_analyse.model.dto.CommonContactDto;
import com.hts_analyse.model.dto.CommonContactMultiShortDto;
import com.hts_analyse.model.dto.CommonContactShortDto;
import com.hts_analyse.model.dto.GeocodingResult;
import com.hts_analyse.model.dto.GroupedResult;
import com.hts_analyse.model.dto.HtsAnalyseDto;
import com.hts_analyse.model.dto.HtsRecordDto;
import com.hts_analyse.model.dto.HtsRecordGroupedDto;
import com.hts_analyse.model.dto.MutualContactRecordDto;
import com.hts_analyse.model.dto.RecordTypeTimelineDto;
import com.hts_analyse.model.record.HtsPairsResponse;
import com.hts_analyse.model.record.MultiGsmEvent;
import com.hts_analyse.model.record.MultiGsmGroupedResult;
import com.hts_analyse.model.record.NearbyBazKey;
import com.hts_analyse.model.record.PairKey;
import com.hts_analyse.model.response.CommonContactMultiResponse;
import com.hts_analyse.model.response.CommonContactResponse;
import com.hts_analyse.util.GeoUtils;
import com.hts_analyse.util.GsmValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HtsAnalyseService {

    private final GeocodingService geocodingService;
    private final HtsRecordQueryService htsRecordQueryService;

    public List<GroupedResult> analyseDistance(String baseGsmNumber, List<String> compareGsmNumbers, int minute, int distance,
            LocalDateTime startDate, LocalDateTime endDate) {
        return HtsAnalyseGrouper.groupByStationsAndDay(
                analyseRawDistance(baseGsmNumber, compareGsmNumbers, minute, distance, startDate, endDate)
        );
    }

    private List<HtsAnalyseDto> analyseRawDistance(String baseGsmNumber, List<String> compareGsmNumbers, int minute, int distance,
            LocalDateTime startDate, LocalDateTime endDate) {
        List<HtsRecordEntity> recordEntities = findHtsRecordsByGsmNumber(baseGsmNumber, startDate, endDate);

        Map<Long, List<HtsRecordEntity>> recordsMap = new HashMap<>();

        for (HtsRecordEntity htsRecordEntity : recordEntities) {
            List<HtsRecordEntity> otherRecordEntities = findHtsRecordsByGsmNumberAndRecordTimeAndDistance(compareGsmNumbers,
                    htsRecordEntity.getRecordDatetime(), minute, htsRecordEntity.getLatitude(), htsRecordEntity.getLongitude(), distance);

            if (!otherRecordEntities.isEmpty()) {
                recordsMap.put(htsRecordEntity.getId(), otherRecordEntities);
            }
        }

        return cretaeHtsAnalyseDtoList(recordsMap, recordEntities, distance);
    }

    public HtsPairsResponse analyseNetworkPairs(
            List<String> comparableGsmNumbers,
            int minute,
            int distance,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<String> allGsmNumbers = buildAllGsmNumbers(comparableGsmNumbers);

        Map<PairKey, List<GroupedResult>> othersPairs = new LinkedHashMap<>();
        Map<PairKey, List<HtsAnalyseDto>> pairMatches = new LinkedHashMap<>();

        for (int i = 0; i < allGsmNumbers.size(); i++) {
            for (int j = i + 1; j < allGsmNumbers.size(); j++) {
                String a = allGsmNumbers.get(i);
                String b = allGsmNumbers.get(j);

                List<HtsAnalyseDto> rawMatches = analyseRawDistance(a, List.of(b), minute, distance, startDate, endDate);
                if (!rawMatches.isEmpty()) {
                    PairKey key = new PairKey(a, b);
                    pairMatches.put(key, rawMatches);
                    othersPairs.put(key, HtsAnalyseGrouper.groupByStationsAndDay(rawMatches));
                }
            }
        }

        List<MultiGsmGroupedResult> allGroups = buildMultiGsmGroups(pairMatches);
        return new HtsPairsResponse(allGsmNumbers, othersPairs, allGroups);
    }

    public List<HtsRecordDto> findNearbyBazRecords(String address, List<String> gsmNumbers, int distance,
            LocalDateTime startTime, LocalDateTime endTime) {
        GeocodingResult geocodingResult = geocodingService.geocode(address);
        if (geocodingResult == null) return Collections.emptyList();

        double lat = geocodingResult.getLatitude();
        double lon = geocodingResult.getLongitude();
     //   double lat = 37.83622031155518;//geocodingResult.getLatitude();
     //   double lon = 27.848970126501168;//geocodingResult.getLongitude();


        return HtsRecordMapper.toDtoList(
                htsRecordQueryService.findNearbyRecords(gsmNumbers, startTime, endTime, lat, lon, distance)
        );
    }

    public List<HtsRecordGroupedDto> findNearbyBazRecordsGrouped(String address, List<String> gsmNumbers, int distance,
            LocalDateTime startTime, LocalDateTime endTime) {
        List<HtsRecordDto> rawRecords = findNearbyBazRecords(address, gsmNumbers, distance, startTime, endTime);
        if (rawRecords.isEmpty()) return Collections.emptyList();

        Map<NearbyBazKey, List<HtsRecordDto>> groupedByGsmAndLocation = rawRecords.stream()
                .collect(Collectors.groupingBy(
                        this::toNearbyBazKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupedByGsmAndLocation.values().stream()
                .map(this::toGroupedDtoWithTimelines)
                .sorted(Comparator
                        .comparing(HtsRecordGroupedDto::getGsmNumber, Comparator.nullsLast(String::compareTo))
                        .thenComparing(HtsRecordGroupedDto::getTotalRecordCount, Comparator.reverseOrder())
                        .thenComparing(HtsRecordGroupedDto::getFirstSeen, Comparator.nullsLast(LocalDateTime::compareTo))
                )
                .toList();
    }

    private NearbyBazKey toNearbyBazKey(HtsRecordDto dto) {
        String gsmNumber = dto.getGsmNumber();

        String locationKey = StringUtils.isNotBlank(dto.getBaseStationId())
                ? "BS:" + dto.getBaseStationId()
                : "AD:" + normalizeAddress(dto.getAddress());

        return new NearbyBazKey(gsmNumber, locationKey);
    }

    private HtsRecordGroupedDto toGroupedDtoWithTimelines(List<HtsRecordDto> group) {
        HtsRecordDto sample = group.getFirst();

        List<RecordTypeTimelineDto> timelines = group.stream()
                .collect(Collectors.groupingBy(
                        dto -> StringUtils.defaultIfBlank(dto.getRecordType(), "UNKNOWN"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> toTimeline(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RecordTypeTimelineDto::getRecordType))
                .toList();

        LocalDateTime firstSeen = timelines.stream()
                .map(RecordTypeTimelineDto::getFirstSeen)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastSeen = timelines.stream()
                .map(RecordTypeTimelineDto::getLastSeen)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return HtsRecordGroupedDto.builder()
                .gsmNumber(sample.getGsmNumber())
                .baseStationId(sample.getBaseStationId())
                .operator(sample.getOperator())
                .address(sample.getAddress())
                .city(sample.getCity())
                .district(sample.getDistrict())
                .latitude(sample.getLatitude())
                .longitude(sample.getLongitude())
                .totalRecordCount(group.size())
                .firstSeen(firstSeen)
                .lastSeen(lastSeen)
                .timelines(timelines)
                .build();
    }

    private RecordTypeTimelineDto toTimeline(String recordType, List<HtsRecordDto> records) {
        List<LocalDateTime> times = records.stream()
                .map(HtsRecordDto::getRecordDatetime)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        LocalDateTime firstSeen = times.isEmpty() ? null : times.get(0);
        LocalDateTime lastSeen = times.isEmpty() ? null : times.get(times.size() - 1);

        return RecordTypeTimelineDto.builder()
                .recordType(recordType)
                .recordCount(records.size())
                .firstSeen(firstSeen)
                .lastSeen(lastSeen)
                .recordDatetimes(times)
                .build();
    }

    private String normalizeAddress(String address) {
        if (StringUtils.isBlank(address)) return "";
        return address.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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

    public CommonContactMultiResponse findCommonContactsMulti(
            List<String> gsmNumbers,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (gsmNumbers == null || gsmNumbers.size() < 2) {
            return CommonContactMultiResponse.builder()
                    .totalCommonCount(0)
                    .commonCommunications(List.of())
                    .build();
        }

        List<String> normalizedGsms = gsmNumbers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        Map<String, Map<String, List<HtsRecordEntity>>> contactToGsmRecords = new HashMap<>();

        for (String gsm : normalizedGsms) {
            List<HtsRecordEntity> records;
            if (startTime != null && endTime != null) {
                records = htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeBetween(gsm, startTime, endTime);
            } else if (startTime != null) {
                records = htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeAfter(gsm, startTime);
            } else if (endTime != null) {
                records = htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeBefore(gsm, endTime);
            } else {
                records = htsRecordQueryService.findAllByGsmNumber(gsm);
            }
            Map<String, List<HtsRecordEntity>> byOther = groupByOtherNumber(records);
            for (Map.Entry<String, List<HtsRecordEntity>> entry : byOther.entrySet()) {
                String other = entry.getKey();
                if (!GsmValidator.isValid(other)) {
                    continue;
                }
                contactToGsmRecords
                        .computeIfAbsent(other, k -> new HashMap<>())
                        .put(gsm, entry.getValue());
            }
        }

        List<CommonContactMultiShortDto> list = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<HtsRecordEntity>>> entry : contactToGsmRecords.entrySet()) {
            Map<String, List<HtsRecordEntity>> gsmMap = entry.getValue();
            if (gsmMap.size() < 2) {
                continue;
            }
            if (!entry.getKey().startsWith("5")) {
                continue;
            }

            String commonGsms = normalizedGsms.stream()
                    .filter(gsmMap::containsKey)
                    .collect(Collectors.joining("_"));

            List<HtsRecordEntity> allRecords = gsmMap.values().stream()
                    .flatMap(List::stream)
                    .toList();

            String identityNo = null;
            String fullName = null;
            for (HtsRecordEntity e : allRecords) {
                if (identityNo == null && e.getIdentityNo() != null) identityNo = e.getIdentityNo();
                if (fullName == null && e.getFullName() != null) fullName = e.getFullName();
                if (identityNo != null && fullName != null) break;
            }

            long totalCount = gsmMap.values().stream().mapToLong(List::size).sum();

            list.add(CommonContactMultiShortDto.builder()
                    .commonGsms(commonGsms)
                    .gsm(entry.getKey())
                    .identityNo(identityNo)
                    .fullName(fullName)
                    .totalCommunicationCount(totalCount)
                    .build());
        }

        return CommonContactMultiResponse.builder()
                .totalCommonCount(list.size())
                .commonCommunications(list)
                .build();
    }

    public List<MutualContactRecordDto> findMutualContacts(
            List<String> gsmNumbers,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (gsmNumbers == null || gsmNumbers.size() < 2) {
            return List.of();
        }

        List<String> normalizedGsms = gsmNumbers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        List<HtsRecordEntity> records;
        if (startTime != null && endTime != null) {
            records = htsRecordQueryService.findAllByGsmNumbersAndOtherNumbersBetween(normalizedGsms, startTime, endTime);
        } else if (startTime != null) {
            records = htsRecordQueryService.findAllByGsmNumbersAndOtherNumbersAfter(normalizedGsms, startTime);
        } else if (endTime != null) {
            records = htsRecordQueryService.findAllByGsmNumbersAndOtherNumbersBefore(normalizedGsms, endTime);
        } else {
            records = htsRecordQueryService.findAllByGsmNumbersAndOtherNumbers(normalizedGsms);
        }

        Map<String, List<HtsRecordEntity>> byPair = records.stream()
                .filter(r -> r.getOtherNumber() != null)
                .filter(r -> !r.getGsmNumber().equals(r.getOtherNumber()))
                .collect(Collectors.groupingBy(r -> pairKey(r.getGsmNumber(), r.getOtherNumber())));

        List<MutualContactRecordDto> result = new ArrayList<>();

        for (Map.Entry<String, List<HtsRecordEntity>> entry : byPair.entrySet()) {
            Map<String, List<HtsRecordEntity>> byDirection = entry.getValue().stream()
                    .collect(Collectors.groupingBy(r -> r.getGsmNumber() + "->" + r.getOtherNumber()));

            List<HtsRecordEntity> selected = null;
            for (List<HtsRecordEntity> list : byDirection.values()) {
                if (selected == null) {
                    selected = list;
                } else if (list.size() > selected.size()) {
                    selected = list;
                }
            }

            if (selected != null) {
                for (HtsRecordEntity r : selected) {
                    result.add(MutualContactRecordDto.builder()
                            .gsmNumber(r.getGsmNumber())
                            .otherNumber(r.getOtherNumber())
                            .recordType(r.getRecordType())
                            .recordTime(r.getRecordDatetime())
                            .baseStationInfo(buildBaseStationInfo(r))
                            .build());
                }
            }
        }

        return result;
    }

    private String pairKey(String a, String b) {
        if (a == null || b == null) return "";
        return a.compareTo(b) <= 0 ? a + "_" + b : b + "_" + a;
    }

    private String buildBaseStationInfo(HtsRecordEntity r) {
        List<String> parts = new ArrayList<>();
        if (r.getBaseStationId() != null && !r.getBaseStationId().isBlank()) parts.add(r.getBaseStationId());
        if (r.getOperator() != null && !r.getOperator().isBlank()) parts.add(r.getOperator());
        if (r.getAddress() != null && !r.getAddress().isBlank()) parts.add(r.getAddress());
        if (r.getLatitude() != null) parts.add(String.valueOf(r.getLatitude()));
        if (r.getLongitude() != null) parts.add(String.valueOf(r.getLongitude()));
        return String.join(" - ", parts);
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

    private List<HtsAnalyseDto> cretaeHtsAnalyseDtoList(Map<Long, List<HtsRecordEntity>> recordsMap,
            List<HtsRecordEntity> recordEntities, int distance) {
        List<HtsAnalyseDto> htsAnalyseDtoList = new ArrayList<>();

        for (HtsRecordEntity htsRecordEntity : recordEntities) {
            List<HtsRecordEntity> otherRecordEntities = recordsMap.get(htsRecordEntity.getId());
            if (otherRecordEntities != null) {
                for (HtsRecordEntity otherHtsRecordEntity : otherRecordEntities) {
                    htsAnalyseDtoList.add(HtsAnalyseDto.builder()
                            .baseGsmNumber(htsRecordEntity.getGsmNumber())
                            .distance(GeoUtils.calculateDistance(
                                    htsRecordEntity.getLatitude(), htsRecordEntity.getLongitude(),
                                    otherHtsRecordEntity.getLatitude(), otherHtsRecordEntity.getLongitude()))
                            .baseGsmDateTime(htsRecordEntity.getRecordDatetime())
                            .otherGsmAddress(otherHtsRecordEntity.getAddress())
                            .otherGsmDateTime(otherHtsRecordEntity.getRecordDatetime())
                            .otherGsmNumber(otherHtsRecordEntity.getGsmNumber())
                            .baseGsmAddress(htsRecordEntity.getAddress())
                            .baseStationId(htsRecordEntity.getBaseStationId())
                            .otherStationId(otherHtsRecordEntity.getBaseStationId())
                            .baseLatitude(htsRecordEntity.getLatitude())
                            .baseLongitude(htsRecordEntity.getLongitude())
                            .otherLatitude(otherHtsRecordEntity.getLatitude())
                            .otherLongitude(otherHtsRecordEntity.getLongitude())
                            .build());
                }
            }
        }

        return filterListByDistance(htsAnalyseDtoList, distance);
    }

    private List<HtsAnalyseDto> filterListByDistance(List<HtsAnalyseDto> htsAnalyseDtoList, int distance) {
        Predicate<HtsAnalyseDto> isFartherThanDistance = dto -> dto.getDistance() <= distance;
        return htsAnalyseDtoList.stream()
                .filter(isFartherThanDistance)
                .toList();
    }

    private List<HtsRecordEntity> findHtsRecordsByGsmNumber(String gsmNumber, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeBetween(
                    gsmNumber, startDate, endDate);
        } else if (startDate != null) {
            return htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeAfter(
                    gsmNumber, startDate);
        } else if (endDate != null) {
            return htsRecordQueryService.findAllByGsmNumberAndRecordDatetimeBefore(
                    gsmNumber, endDate);
        } else {
            return htsRecordQueryService.findAllByGsmNumber(gsmNumber);
        }
    }

    private List<HtsRecordEntity> findHtsRecordsByGsmNumberAndRecordTimeAndDistance(List<String> compareGsmNumbers,
            LocalDateTime recordTime, int minute,
            Double latitude, Double longitude, int distance) {
        LocalDateTime startTime = recordTime.minusMinutes(minute);
        LocalDateTime endTime = recordTime.plusMinutes(minute);
        return htsRecordQueryService.findNearbyRecords(compareGsmNumbers, startTime, endTime, latitude, longitude, distance);
    }

    private List<String> buildAllGsmNumbers(List<String> comparableGsmNumbers) {
        if (comparableGsmNumbers == null) {
            return List.of();
        }

        return comparableGsmNumbers.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<MultiGsmGroupedResult> buildMultiGsmGroups(Map<PairKey, List<HtsAnalyseDto>> pairMatches) {
        if (pairMatches.isEmpty()) {
            return List.of();
        }

        Map<EventNodeKey, Set<EventNodeKey>> graph = new LinkedHashMap<>();
        Map<EventNodeKey, EventNodeData> nodes = new LinkedHashMap<>();

        for (List<HtsAnalyseDto> matches : pairMatches.values()) {
            for (HtsAnalyseDto dto : matches) {
                EventNodeData baseNode = EventNodeData.fromBase(dto);
                EventNodeData otherNode = EventNodeData.fromOther(dto);

                nodes.putIfAbsent(baseNode.key(), baseNode);
                nodes.putIfAbsent(otherNode.key(), otherNode);

                graph.computeIfAbsent(baseNode.key(), key -> new LinkedHashSet<>()).add(otherNode.key());
                graph.computeIfAbsent(otherNode.key(), key -> new LinkedHashSet<>()).add(baseNode.key());
            }
        }

        Set<EventNodeKey> visited = new HashSet<>();
        List<MultiGsmGroupedResult> groups = new ArrayList<>();

        for (EventNodeKey startKey : nodes.keySet()) {
            if (!visited.add(startKey)) {
                continue;
            }

            List<EventNodeData> component = new ArrayList<>();
            ArrayList<EventNodeKey> stack = new ArrayList<>();
            stack.add(startKey);

            while (!stack.isEmpty()) {
                EventNodeKey current = stack.remove(stack.size() - 1);
                component.add(nodes.get(current));

                for (EventNodeKey neighbor : graph.getOrDefault(current, Set.of())) {
                    if (visited.add(neighbor)) {
                        stack.add(neighbor);
                    }
                }
            }

            MultiGsmGroupedResult group = toMultiGsmGroupedResult(component);
            if (group != null) {
                groups.add(group);
            }
        }

        return groups.stream()
                .sorted(Comparator
                        .comparing(MultiGsmGroupedResult::gsmCount, Comparator.reverseOrder())
                        .thenComparing(MultiGsmGroupedResult::firstSeen, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(group -> String.join("_", group.gsmNumbers())))
                .toList();
    }

    private MultiGsmGroupedResult toMultiGsmGroupedResult(List<EventNodeData> component) {
        List<String> gsmNumbers = component.stream()
                .map(EventNodeData::gsmNumber)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .toList();

        if (gsmNumbers.size() < 2) {
            return null;
        }

        List<MultiGsmEvent> events = component.stream()
                .sorted(Comparator
                        .comparing(EventNodeData::recordTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(EventNodeData::gsmNumber, Comparator.nullsLast(String::compareTo)))
                .map(node -> MultiGsmEvent.builder()
                        .gsmNumber(node.gsmNumber())
                        .recordTime(node.recordTime())
                        .address(node.address())
                        .stationId(node.stationId())
                        .latitude(node.latitude())
                        .longitude(node.longitude())
                        .build())
                .toList();

        Set<String> stationIds = component.stream()
                .map(EventNodeData::stationId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> addresses = component.stream()
                .map(EventNodeData::address)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Double> latitudes = component.stream()
                .map(EventNodeData::latitude)
                .filter(Objects::nonNull)
                .toList();

        List<Double> longitudes = component.stream()
                .map(EventNodeData::longitude)
                .filter(Objects::nonNull)
                .toList();

        LocalDateTime firstSeen = component.stream()
                .map(EventNodeData::recordTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastSeen = component.stream()
                .map(EventNodeData::recordTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return MultiGsmGroupedResult.builder()
                .gsmNumbers(gsmNumbers)
                .gsmCount(gsmNumbers.size())
                .stationIds(stationIds)
                .addresses(addresses)
                .centerLatitude(latitudes.isEmpty() ? null : latitudes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .centerLongitude(longitudes.isEmpty() ? null : longitudes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .firstSeen(firstSeen)
                .lastSeen(lastSeen)
                .totalEvents(events.size())
                .events(events)
                .build();
    }

    private record EventNodeKey(String gsmNumber, LocalDateTime recordTime, long latitude, long longitude) {}

    private record EventNodeData(
            EventNodeKey key,
            String gsmNumber,
            LocalDateTime recordTime,
            String address,
            String stationId,
            Double latitude,
            Double longitude
    ) {
        private static EventNodeData fromBase(HtsAnalyseDto dto) {
            return new EventNodeData(
                    new EventNodeKey(dto.getBaseGsmNumber(), dto.getBaseGsmDateTime(), normalizeCoordinate(dto.getBaseLatitude()), normalizeCoordinate(dto.getBaseLongitude())),
                    dto.getBaseGsmNumber(),
                    dto.getBaseGsmDateTime(),
                    dto.getBaseGsmAddress(),
                    dto.getBaseStationId(),
                    dto.getBaseLatitude(),
                    dto.getBaseLongitude()
            );
        }

        private static EventNodeData fromOther(HtsAnalyseDto dto) {
            return new EventNodeData(
                    new EventNodeKey(dto.getOtherGsmNumber(), dto.getOtherGsmDateTime(), normalizeCoordinate(dto.getOtherLatitude()), normalizeCoordinate(dto.getOtherLongitude())),
                    dto.getOtherGsmNumber(),
                    dto.getOtherGsmDateTime(),
                    dto.getOtherGsmAddress(),
                    dto.getOtherStationId(),
                    dto.getOtherLatitude(),
                    dto.getOtherLongitude()
            );
        }
    }

    private static long normalizeCoordinate(Double coordinate) {
        return coordinate == null ? 0L : Math.round(coordinate * 100_000);
    }

    public List<Object[]> findLastNamesWithCount(String baseGsmNumber) {
        return htsRecordQueryService.findLastNamesWithCount(baseGsmNumber);
    }

    public List<Object[]> findFullNameIdentityNoWithCount(String gsmNumber, String lastName) {
        return htsRecordQueryService.findFullNameIdentityNoWithCount(gsmNumber, lastName);
    }
}
