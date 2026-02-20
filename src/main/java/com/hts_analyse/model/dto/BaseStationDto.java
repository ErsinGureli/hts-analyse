package com.hts_analyse.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseStationDto {
    private String baseStationId;
    private String operator;
    private String address;
    private Double latitude;
    private Double longitude;
}
