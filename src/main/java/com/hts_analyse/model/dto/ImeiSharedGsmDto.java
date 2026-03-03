package com.hts_analyse.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImeiSharedGsmDto {
    private String imei;
    private int gsmCount;
    private List<String> gsmNumbers;
}
