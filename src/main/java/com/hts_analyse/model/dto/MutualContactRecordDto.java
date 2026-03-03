package com.hts_analyse.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MutualContactRecordDto {
    private String gsmNumber;
    private String otherNumber;
    private String recordType;
    private LocalDateTime recordTime;
    private String baseStationInfo;
}
