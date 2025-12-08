package com.hts_analyse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hts_analyse.util.GeoUtils;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void haversine_correct_and_swapped() {
        double baseLat  = 41.0119435;
        double baseLon  = 29.0752319;
        double otherLat = 41.0160615;
        double otherLon = 29.0747173;

        double correct = GeoUtils.calculateDistance(baseLat, baseLon, otherLat, otherLon);
        double swappedBoth = GeoUtils.calculateDistance(baseLon, baseLat, otherLon, otherLat);
        double swappedFirstOnly = GeoUtils.calculateDistance(baseLon, baseLat, otherLat, otherLon);
        double swappedSecondOnly = GeoUtils.calculateDistance(baseLat, baseLon, otherLon, otherLat);

        assertEquals(459.932, correct, 0.5);
        assertEquals(404.268, swappedBoth, 0.5);
        assertEquals(1_712_262, swappedFirstOnly, 10);  // ~1712 km
        assertEquals(1_712_179, swappedSecondOnly, 10); // ~1712 km
    }
}
