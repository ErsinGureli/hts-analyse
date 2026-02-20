package com.hts_analyse.model.record;

public record PairKey(String a, String b) {

    public PairKey {
        // null guard (isteğe bağlı)
        if (a == null || b == null) {
            throw new IllegalArgumentException("PairKey values cannot be null");
        }

        // canonical order: (a,b) her zaman aynı sırada olsun (duplicate riskini iyice azaltır)
        if (a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
    }
}