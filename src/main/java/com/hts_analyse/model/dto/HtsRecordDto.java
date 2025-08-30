package com.hts_analyse.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HtsRecordDto {

    private Long id;
    private String gsmNumber;
    private String recordType;
    private String otherNumber;
    private LocalDateTime recordDatetime;
    private String fullName;
    private String baseStationId;
    private String operator;
    private String address;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private String identityNo;
    private String imei;
}
