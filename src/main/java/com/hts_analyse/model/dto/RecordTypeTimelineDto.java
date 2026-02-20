package com.hts_analyse.model.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordTypeTimelineDto {
    private String recordType;
    private int recordCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private List<LocalDateTime> recordDatetimes;
}
