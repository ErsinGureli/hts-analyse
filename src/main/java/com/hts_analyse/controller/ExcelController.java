package com.hts_analyse.controller;

import com.hts_analyse.model.CityDistrict;
import com.hts_analyse.model.ExcelRecord;
import com.hts_analyse.service.CityLoaderService;
import com.hts_analyse.service.ExcelImportService;
import com.hts_analyse.service.ExcelReaderService;
import com.hts_analyse.service.SimpleAddressParser;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelReaderService excelReaderService;
    private final SimpleAddressParser simpleAddressParser;
    private final CityLoaderService cityLoaderService;
    private final ExcelImportService excelImportService;

    @GetMapping("/read")
    public List<ExcelRecord> readExcel(@RequestParam String filePath) {
        return excelReaderService.readExcel(filePath);
    }

    @GetMapping("/analyse")
    public List<ExcelRecord> analyseExcel(@RequestParam String filePath) {
        List<ExcelRecord> excelRecords = excelReaderService.readExcel(filePath);
        Map<String, Set<String>> loadedCityMap = cityLoaderService.getCityMap();

        int filledCityCounter = 0;
        for (ExcelRecord kayit : excelRecords) {
            System.out.println(kayit.getBaseStation().getAddress());
            CityDistrict cityDistrict = simpleAddressParser.parseAdres(kayit.getBaseStation().getAddress(), loadedCityMap);

            if(cityDistrict.getDistrict() != null) {
                filledCityCounter++;
            }

            System.out.println(cityDistrict);

            System.out.println( "-----------");
        }
        System.out.println("Record count:" + excelRecords.size());
        System.out.println("filledCityCount: " + filledCityCounter);
        return excelRecords;
    }

    @PostMapping("/record")
    public ResponseEntity<String> recordExcel(@RequestParam String filePath) {
        excelImportService.importExcel(filePath);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/importExcelWithFreeAPI")
    public ResponseEntity<String> importExcelWithFreeApi(@RequestParam String filePath) {
        excelImportService.importExcelTests(filePath);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/checkFreeApi")
    public ResponseEntity<String> checkAddressWithFreeApi(@RequestParam String address) {
        return ResponseEntity.ok(excelImportService.checkFreeApi(address));
    }

    @PostMapping("/checkGeoapifyGeocoderService")
    public ResponseEntity<String> checkAddressWithGeoapifyGeocoderService(@RequestParam String address) {
        return ResponseEntity.ok(excelImportService.checkByGeoapify(address));
    }

}