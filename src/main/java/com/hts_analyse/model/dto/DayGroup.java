package com.hts_analyse.model.dto;

import com.hts_analyse.model.record.Pair;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DayGroup {
    private String date;
    private int count;
    private List<Pair> pairs;
}