package com.hts_analyse.model.record;

import com.hts_analyse.model.dto.GroupedResult;
import java.util.List;
import java.util.Map;

public record HtsPairsResponse(
        List<String> gsmNumbers,
        Map<PairKey, List<GroupedResult>> othersPairs,
        List<MultiGsmGroupedResult> allGroups
) {}
