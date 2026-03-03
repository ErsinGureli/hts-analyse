package com.hts_analyse.controller;

import com.hts_analyse.model.response.ImeiSharedGsmResponse;
import com.hts_analyse.service.ImeiAnalyseService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hts/imei")
@RequiredArgsConstructor
public class ImeiAnalyseController {

    private final ImeiAnalyseService imeiAnalyseService;

    @GetMapping("/common-imeis")
    public ResponseEntity<ImeiSharedGsmResponse> getCommonImeis(
            @RequestParam List<String> gsmNumbers,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(imeiAnalyseService.findSharedImeis(gsmNumbers, startTime, endTime));
    }

    @PostMapping("/sync-gsm-imei")
    public ResponseEntity<Map<String, Object>> syncGsmImei(@RequestParam String gsmNumber) {
        int insertedCount = imeiAnalyseService.syncGsmImeiForGsm(gsmNumber);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gsmNumber", gsmNumber);
        response.put("insertedCount", insertedCount);

        return ResponseEntity.ok(response);
    }
}
