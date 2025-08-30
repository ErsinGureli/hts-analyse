package com.hts_analyse.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupedResult {
    private String baseGsmNumber;
    private String otherGsmNumber;
    private String baseStationId;
    private String otherStationId;
    private String baseAddress;
    private String otherAddress;
    private int totalPairs;
    private double distanceMeters;
    private List<DayGroup> byDay;
}