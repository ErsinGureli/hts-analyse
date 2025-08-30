package com.hts_analyse.model.record;

public record GroupKey(String baseStationId, String otherStationId,
                       String baseGsmNumber, String otherGsmNumber) {}