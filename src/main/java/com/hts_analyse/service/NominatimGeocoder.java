package com.hts_analyse.service;

import com.hts_analyse.model.dto.Location;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NominatimGeocoder {

    public Location geocode(String address) throws Exception {
        String url = "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q="
                + URLEncoder.encode(address, StandardCharsets.UTF_8);
        Thread.sleep(1000);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "JavaApp"); // zorunlu
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONArray arr = new JSONArray(response.toString());
        if (arr.length() == 0) {
            return null;  // sonuç yok
        }

        JSONObject obj = arr.getJSONObject(0); // en iyi eşleşme
        Location loc = new Location();
        loc.setDisplayName(obj.getString("display_name"));
        loc.setLatitude(obj.getDouble("lat"));
        loc.setLongitude(obj.getDouble("lon"));

        JSONObject jsonAddress = obj.getJSONObject("address");

        if (jsonAddress.has("city")) {
            loc.setCity(jsonAddress.getString("city"));
        } else if (jsonAddress.has("town")) {
            loc.setCity(jsonAddress.getString("town"));
        } else if (jsonAddress.has("village")) {
            loc.setCity(jsonAddress.getString("village"));
        } else if (jsonAddress.has("municipality")) {
            loc.setCity(jsonAddress.getString("municipality"));
        } else if (jsonAddress.has("county")) {
            loc.setCity(jsonAddress.getString("county"));
        } else if (jsonAddress.has("suburb")) {
            loc.setCity(jsonAddress.getString("suburb"));
        } else {
            loc.setCity( "");
        }

        loc.setState(jsonAddress.has("state") ? jsonAddress.getString("state") : "");

        return loc;
    }
}