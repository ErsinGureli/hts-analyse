package com.hts_analyse.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GsmImeiDto {
    private String gsm;
    private String imei;
}