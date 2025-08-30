package com.hts_analyse.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeocodingResult {
    private double latitude;
    private double longitude;
    private String city;     // İl
    private String district; // İlçe
}