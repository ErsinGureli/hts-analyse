package com.hts_analyse.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelRecord {
    private String gsmNumber;
    private String recordType;
    private String otherNumber;
    private String recordTime;
    private String fullName;
    private BaseStationDto baseStation;
}
