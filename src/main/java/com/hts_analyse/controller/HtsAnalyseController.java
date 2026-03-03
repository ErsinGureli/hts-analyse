package com.hts_analyse.controller;

import com.hts_analyse.model.dto.GroupedResult;
import com.hts_analyse.model.dto.HtsRecordGroupedDto;
import com.hts_analyse.model.record.HtsPairsResponse;
import com.hts_analyse.model.dto.MutualContactRecordDto;
import com.hts_analyse.model.response.CommonContactMultiResponse;
import com.hts_analyse.model.response.CommonContactResponse;
import com.hts_analyse.service.CommonContactExcelService;
import com.hts_analyse.service.GoogleMapPageRenderer;
import com.hts_analyse.service.HtsAnalyseService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final CommonContactExcelService commonContactExcelService;

    @Value("${google.maps-api.key}")
    private String mapsApiKey;

    @GetMapping
    public ResponseEntity<List<GroupedResult>> analyse(
            @RequestParam String baseGsmNumber,
            @RequestParam List<String> comparableGsmNumbers,
            @RequestParam(required = false, defaultValue = "60") int minute,
            @RequestParam(required = false, defaultValue = "1000") int distance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {

        return ResponseEntity.ok(
                htsAnalyseService.analyseDistance(baseGsmNumber, comparableGsmNumbers, minute, distance, startDate, endDate)
        );
    }

    @GetMapping("/api/v1/hts/analyse/pairs")
    public ResponseEntity<HtsPairsResponse> analysePairs(
            @RequestParam String baseGsmNumber,
            @RequestParam List<String> comparableGsmNumbers,
            @RequestParam(required = false, defaultValue = "60") int minute,
            @RequestParam(required = false, defaultValue = "1000") int distance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {
        return ResponseEntity.ok(
                htsAnalyseService.analyseNetworkPairs(baseGsmNumber, comparableGsmNumbers, minute, distance, startDate, endDate)
        );
    }

    @GetMapping(value = "/map", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> analyseMap(
            @RequestParam String baseGsmNumber,
            @RequestParam List<String> comparableGsmNumbers,
            @RequestParam(defaultValue = "60") int minute,
            @RequestParam(defaultValue = "1000") int distance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "false") boolean open) {

        List<GroupedResult> data = htsAnalyseService
                .analyseDistance(baseGsmNumber, comparableGsmNumbers, minute, distance, startDate, endDate);

        String html = GoogleMapPageRenderer.render(data, mapsApiKey);

        if (open && java.awt.Desktop.isDesktopSupported() && !java.awt.GraphicsEnvironment.isHeadless()) {
            try {
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile("hts-map-", ".html");
                java.nio.file.Files.writeString(tmp, html, java.nio.charset.StandardCharsets.UTF_8);
                java.awt.Desktop.getDesktop().browse(tmp.toUri());
            } catch (Exception e) {
                // Sessizce geçmek yerine logla, ama response’u yine de dön:
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Auto-open failed", e);
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }


    @GetMapping("/nearby-baz-records")
    public ResponseEntity<List<HtsRecordGroupedDto>> findNearbyBazRecords(
            @RequestParam String address,
            @RequestParam List<String> gsmNumbers,
            @RequestParam int distance,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,   //2023-03-11T00:00:00.000
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<HtsRecordGroupedDto> result = htsAnalyseService.findNearbyBazRecordsGrouped(address, gsmNumbers, distance, startTime, endTime);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/common-contacts")
    public ResponseEntity<CommonContactResponse> getCommonContacts(
            @RequestParam String gsm1,
            @RequestParam String gsm2) {
        return ResponseEntity.ok(htsAnalyseService.findCommonContacts(gsm1, gsm2));
    }

    @GetMapping("/common-contacts-as-excel")
    public ResponseEntity<byte[]> downloadCommonContactsExcel(
            @RequestParam String gsm1,
            @RequestParam String gsm2) {

        CommonContactResponse response = htsAnalyseService.findCommonContacts(gsm1, gsm2);
        byte[] excelBytes = commonContactExcelService.generateExcel(response);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("common_contacts_" + gsm1 + "_" + gsm2 + ".xlsx").build());

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/common-contacts-as-excel-multi")
    public ResponseEntity<byte[]> downloadCommonContactsExcelMulti(
            @RequestParam List<String> gsmNumbers,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        CommonContactMultiResponse response = htsAnalyseService.findCommonContactsMulti(gsmNumbers, startTime, endTime);
        byte[] excelBytes = commonContactExcelService.generateMultiExcel(response);

        String filename = "common_contacts_" + String.join("_", gsmNumbers) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/mutual-contacts")
    public ResponseEntity<List<MutualContactRecordDto>> getMutualContacts(
            @RequestParam List<String> gsmNumbers,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(htsAnalyseService.findMutualContacts(gsmNumbers, startTime, endTime));
    }

    @GetMapping("/mutual-contacts-as-excel")
    public ResponseEntity<byte[]> downloadMutualContactsExcel(
            @RequestParam List<String> gsmNumbers,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<MutualContactRecordDto> records = htsAnalyseService.findMutualContacts(gsmNumbers, startTime, endTime);
        byte[] excelBytes = commonContactExcelService.generateMutualContactsExcel(records);

        String filename = "mutual_contacts_" + String.join("_", gsmNumbers) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(excelBytes);
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

    @GetMapping("/most-contacts-last-names")
    public ResponseEntity<List<Map<String, Object>>> getMostContactsLastNamesWithCount(
            @RequestParam String gsmNumber,
            @RequestParam(name = "minCount", defaultValue = "2") int minCount
    ) {
        List<Object[]> results = htsAnalyseService.findLastNamesWithCount(gsmNumber);

        List<Map<String, Object>> response = results.stream()
                // 1) count filtresi
                .filter(row -> {
                    Number count = (Number) row[1];
                    return count != null && count.intValue() >= minCount;
                })
                // 2) count DESC sıralama
                .sorted((r1, r2) -> {
                    Number c1 = (Number) r1[1];
                    Number c2 = (Number) r2[1];
                    return Integer.compare(c2.intValue(), c1.intValue()); // büyükten küçüğe
                })
                // 3) Map'e çevirme
                .map(row -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("last_word", row[0]);
                    map.put("count", row[1]);
                    map.put("person_count", row[2]);
                    return map;
                })
                .toList();

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
