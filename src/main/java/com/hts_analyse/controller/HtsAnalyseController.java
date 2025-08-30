package com.hts_analyse.controller;

import com.hts_analyse.model.dto.GroupedResult;
import com.hts_analyse.model.dto.HtsRecordDto;
import com.hts_analyse.model.response.CommonContactResponse;
import com.hts_analyse.service.HtsAnalyseService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<List<GroupedResult>> analyse(
            @RequestParam String baseGsmNumber,
            @RequestParam List<String> comparableGsmNumbers,
            @RequestParam(required = false, defaultValue = "60") int minute,
            @RequestParam(required = false, defaultValue = "1000") int distance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(
                htsAnalyseService.analyseDistance(baseGsmNumber, comparableGsmNumbers, minute, distance, startDate, endDate)
        );
    }


    @GetMapping("/nearby-baz-records")
    public ResponseEntity<List<HtsRecordDto>> findNearbyBazRecords(
            @RequestParam String address,
            @RequestParam List<String> gsmNumbers,
            @RequestParam int distance,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<HtsRecordDto> result = htsAnalyseService.findNearbyBazRecords(address, gsmNumbers, distance, startTime, endTime);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/common-contacts")
    public ResponseEntity<CommonContactResponse> getCommonContacts(
            @RequestParam String gsm1,
            @RequestParam String gsm2) {
        return ResponseEntity.ok(htsAnalyseService.findCommonContacts(gsm1, gsm2));
    }

    @GetMapping("/last-names")
    public ResponseEntity<List<Map<String, Object>>> getLastNamesWithCount(@RequestParam String gsmNumber) {
        List<Object[]> results = htsAnalyseService.findLastNamesWithCount(gsmNumber);

        List<Map<String, Object>> response = results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("last_word", row[0]);
            map.put("count", row[1]);
            map.put("person_count", row[2]);
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/full-names")
    public ResponseEntity<List<Map<String, Object>>> getFullNamesWithIdentityNoAndCount(
            @RequestParam String gsmNumber,
            @RequestParam String lastName) {

        List<Object[]> results = htsAnalyseService.findFullNameIdentityNoWithCount(gsmNumber, lastName);

        List<Map<String, Object>> response = results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("full_name", row[0]);
            map.put("identity_no", row[1]);
            map.put("count", row[2]);
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/full-names-by-lastname")
    public ResponseEntity<List<Map<String, Object>>> getFullNamesByLastName(
            @RequestParam String gsmNumber,
            @RequestParam(required = false) String lastName) {

        List<Map<String, Object>> responseList = new ArrayList<>();

        if (lastName != null && !lastName.isBlank()) {
            List<Object[]> results = htsAnalyseService.findFullNameIdentityNoWithCount(gsmNumber, lastName);

            List<Map<String, Object>> fullNames = results.stream().map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("full_name", row[0]);
                map.put("count", row[2]);
                map.put("identity_no", row[1]);
                return map;
            }).toList();

            if (!lastName.isBlank()) {
                Map<String, Object> singleResponse = new HashMap<>();
                singleResponse.put("last_word", lastName);
                singleResponse.put("person_count", fullNames.size());
                singleResponse.put("full_names", fullNames);

                responseList.add(singleResponse);
            }
        } else {
            List<Object[]> lastNamesResults = htsAnalyseService.findLastNamesWithCount(gsmNumber);

            for (Object[] lnRow : lastNamesResults) {
                String currentLastName = (String) lnRow[0];

                if (currentLastName == null || currentLastName.isBlank()) {
                    continue;
                }

                List<Object[]> results = htsAnalyseService.findFullNameIdentityNoWithCount(gsmNumber, currentLastName);
                List<Map<String, Object>> fullNames = results.stream().map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("full_name", row[0]);
                    map.put("count", row[2]);
                    map.put("identity_no", row[1]);
                    return map;
                }).toList();

                Map<String, Object> singleResponse = new HashMap<>();
                singleResponse.put("last_word", currentLastName);
                singleResponse.put("person_count", fullNames.size());
                singleResponse.put("full_names", fullNames);

                responseList.add(singleResponse);
            }
        }

        responseList.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("person_count")).reversed());
        return ResponseEntity.ok(responseList);
    }


}
