package com.hts_analyse.model.record;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;

@Builder
public record MultiGsmGroupedResult(
        List<String> gsmNumbers,
        int gsmCount,
        Set<String> stationIds,
        Set<String> addresses,
        Double centerLatitude,
        Double centerLongitude,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        int totalEvents,
        List<MultiGsmEvent> events
) {}
