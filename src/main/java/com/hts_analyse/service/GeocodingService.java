package com.hts_analyse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hts_analyse.model.dto.GeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.create();

    @Value("${google.api.key}")
    private String apiKey;

    public GeocodingResult geocode(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                encodedAddress, apiKey
            );

            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode result = root.path("results").get(0);
            JsonNode location = result.path("geometry").path("location");

            double lat = location.path("lat").asDouble();
            double lng = location.path("lng").asDouble();

            String city = null;
            String district = null;

            for (JsonNode component : result.path("address_components")) {
                JsonNode types = component.path("types");
                if (types.toString().contains("administrative_area_level_1")) {
                    city = component.path("long_name").asText();
                } else if (types.toString().contains("administrative_area_level_2")) {
                    district = component.path("long_name").asText();
                }
            }

            return GeocodingResult.builder()
                    .latitude(lat)
                    .longitude(lng)
                    .city(city)
                    .district(district)
                    .build();

        } catch (Exception e) {
            log.error("Geocoding failed for address '{}': {}", address, e.getMessage());
            return null;
        }
    }
}
