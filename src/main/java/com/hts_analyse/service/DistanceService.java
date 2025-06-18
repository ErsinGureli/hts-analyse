package com.hts_analyse.service;

import com.hts_analyse.model.GeocodingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DistanceService {

    private final GeocodingService geocodingService;

    public double calculateDistanceInKm(String address1, String address2) {
        GeocodingResult loc1 = geocodingService.geocode(address1);
        GeocodingResult loc2 = geocodingService.geocode(address2);

        return haversine(loc1.getLatitude(), loc1.getLongitude(),
                         loc2.getLatitude(), loc2.getLongitude());
    }

    // Haversine Formülü
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Kilometre cinsinden

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2) +
                   Math.cos(Math.toRadians(lat1)) *
                   Math.cos(Math.toRadians(lat2)) *
                   Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS * c;
    }
}
