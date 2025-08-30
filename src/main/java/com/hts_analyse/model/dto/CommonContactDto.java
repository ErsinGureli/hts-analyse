package com.hts_analyse.model.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonContactDto {
    private String gsmNumber1;
    private String gsmNumber2;
    private String commonGsm;
    private long gsm1CommunicationCount;
    private long gsm2CommunicationCount;
    private Map<String, Long> gsm1CommunicationTypeCounts;
    private Map<String, Long> gsm2CommunicationTypeCounts;
}