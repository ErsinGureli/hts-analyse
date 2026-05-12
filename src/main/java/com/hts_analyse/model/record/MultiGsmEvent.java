package com.hts_analyse.model.record;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MultiGsmEvent(
        String gsmNumber,
        LocalDateTime recordTime,
        String address,
        String stationId,
        Double latitude,
        Double longitude
) {}
