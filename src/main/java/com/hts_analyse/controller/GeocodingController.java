package com.hts_analyse.controller;

import com.hts_analyse.model.GeocodingResult;
import com.hts_analyse.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geocode")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping
    public GeocodingResult geocode(@RequestParam String address) {
        return geocodingService.geocode(address);
    }
}