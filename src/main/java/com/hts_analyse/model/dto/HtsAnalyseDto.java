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
public class HtsAnalyseDto {
    private String baseGsmNumber;
    private String otherGsmNumber;
    private String baseGsmAddress;
    private String otherGsmAddress;
    private LocalDateTime baseGsmDateTime;
    private LocalDateTime otherGsmDateTime;
    private Double distance;
    private String baseStationId;
    private String otherStationId;

}
