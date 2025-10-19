package com.hts_analyse.model.record;

import com.hts_analyse.model.dto.HtsAnalyseDto;

public record GroupKey(String baseGsmNumber, String otherGsmNumber, long latRounded, long lonRounded) {
    public static GroupKey from(HtsAnalyseDto dto) {
        return new GroupKey(
                safe(dto.getBaseGsmNumber()),
                safe(dto.getOtherGsmNumber()),
                normalize(dto.getBaseLatitude()),
                normalize(dto.getBaseLongitude())
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static long normalize(Double d) {
        return d == null ? 0L : Math.round(d * 100_000);
    }
}