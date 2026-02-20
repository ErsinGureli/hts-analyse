package com.hts_analyse.model.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HtsRecordGroupedDto {

    private String gsmNumber;

    private String baseStationId;
    private String operator;
    private String address;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;

    private int totalRecordCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;

    private List<RecordTypeTimelineDto> timelines; // recordType bazlı ayrılmış zamanlar
}
