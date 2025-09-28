package com.hts_analyse.model.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupedResult {
    private String baseGsmNumber;
    private String otherGsmNumber;
    private String baseAddress;
    private String otherAddress;
    private Double latitude;
    private Double longitude;
    private Set<String> baseStationIds;
    private Set<String> otherStationIds;
    private int totalPairs;
    private double distanceMeters;
    private List<DayGroup> byDay;
}
