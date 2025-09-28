package com.hts_analyse.model.dto;

import com.hts_analyse.model.record.PairGroup;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DayGroup {
    private String date;
    private List<PairGroup> pairGroups;
    private int count;
}
