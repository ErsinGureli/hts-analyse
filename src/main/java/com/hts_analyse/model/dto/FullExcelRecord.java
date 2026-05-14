package com.hts_analyse.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullExcelRecord {
    private List<ExcelRecord> htsRecords;
    private List<SubscriptionInformationRecord> subscriptionInformations;
}
