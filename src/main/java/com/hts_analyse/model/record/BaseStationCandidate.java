package com.hts_analyse.model.record;

public record BaseStationCandidate(
        String baseStationId,
        String address,
        String addressKey,
        Double latitude,
        Double longitude
) {
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}