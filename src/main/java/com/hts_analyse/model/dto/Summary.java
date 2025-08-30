package com.hts_analyse.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Summary {
    private int totalPairs;
    private double minDistanceMeters;
    private double maxDistanceMeters;
    private double avgDistanceMeters;
}