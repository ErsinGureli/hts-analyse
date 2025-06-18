package com.hts_analyse.controller;

import com.hts_analyse.model.GeocodingResult;
import com.hts_analyse.model.HtsAnalyseDto;
import com.hts_analyse.service.HtsAnalyseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hts/analyse")
@RequiredArgsConstructor
public class HtsAnalyseController {

    private final HtsAnalyseService htsAnalyseService;

    @GetMapping
    public ResponseEntity<List<HtsAnalyseDto>> analyse(
            @RequestParam String baseGsmNumber,
            @RequestParam List<String> comparableGsmNumbers,
            @RequestParam(required = false, defaultValue = "60") int minute,
            @RequestParam(required = false, defaultValue = "1000") int distance) {
        return ResponseEntity.ok(htsAnalyseService.analyseDistance(baseGsmNumber, comparableGsmNumbers, minute, distance));
    }

}
