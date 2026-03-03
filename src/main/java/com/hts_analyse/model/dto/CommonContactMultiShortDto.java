package com.hts_analyse.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonContactMultiShortDto {
    private String commonGsms;
    private String gsm;
    private String identityNo;
    private String fullName;
    private long totalCommunicationCount;
}
