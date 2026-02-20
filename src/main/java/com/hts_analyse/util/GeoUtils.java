package com.hts_analyse.util;

public class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6371000; // Dünya yarıçapı (metre)

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }


    public static void main(String[] args) {
        double baseLat  = 37.7434522;
        double baseLon  = 29.10286409999999;
        double otherLat = 38.4708595;
        double otherLon = 27.0933154;

        double dCorrect = calculateDistance(baseLat, baseLon, otherLat, otherLon);
        double dSwapped = calculateDistance(baseLon, baseLat, otherLon, otherLat); // bilerek lat/lon ters

        System.out.printf("Doğru sırayla (lat,lon → lat,lon): %.3f m%n", dCorrect);
        System.out.printf("Ters sırayla  (lon,lat → lon,lat): %.3f m%n", dSwapped);
    }
}
