package com.hts_analyse.service;

import com.hts_analyse.model.dto.Location;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeoapifyGeocoderService {

    @Value("${geoapify.api.key}")
    private String apiKey;

    public Location geocode(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String urlStr = "https://api.geoapify.com/v1/geocode/search?text=" + encodedAddress
                    + "&format=json&apiKey=" + apiKey;
            Thread.sleep(1000);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("Geoapify API returned non-200 status: {}", status);
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }
            in.close();
            conn.disconnect();

            JSONObject json = new JSONObject(responseBuilder.toString());
            JSONArray features = json.getJSONArray("features");

            if (features.length() == 0) {
                log.warn("No results found for address: {}", address);
                return null;
            }

            JSONObject props = features.getJSONObject(0).getJSONObject("properties");

            Location loc = new Location();
            loc.setDisplayName(props.getString("formatted"));
            loc.setLat(props.getDouble("lat"));
            loc.setLon(props.getDouble("lon"));
            loc.setCity(props.optString("city", props.optString("county", "")));
            loc.setState(props.optString("state", ""));
            return loc;

        } catch (Exception e) {
            log.error("Failed to geocode address: {}", address, e);
            return null;
        }
    }
}
