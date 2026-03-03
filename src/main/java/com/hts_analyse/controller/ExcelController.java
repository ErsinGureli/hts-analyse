package com.hts_analyse.controller;

import com.hts_analyse.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelImportService excelImportService;

    /*@GetMapping("/read")
    public List<ExcelRecord> readExcel(@RequestParam String filePath1, @RequestParam String filePath2) {
        List<ExcelRecord> excelRecords =  excelReaderService.readExcel(filePath1);
        List<ExcelRecord> excelRecords2 =  excelReaderService.readFullExcel(filePath2);

        System.out.println(excelRecords.size());
        System.out.println(excelRecords2.size());

        return excelRecords;
    }*/

    /*@GetMapping("/analyse")
    public List<ExcelRecord> analyseExcel(@RequestParam String filePath) {
        List<ExcelRecord> excelRecords = excelReaderService.readExcel(filePath);
        Map<String, Set<String>> loadedCityMap = cityLoaderService.getCityMap();

        int filledCityCounter = 0;
        for (ExcelRecord kayit : excelRecords) {
            log.info(kayit.getBaseStation().getAddress());
            CityDistrict cityDistrict = simpleAddressParser.parseAdres(kayit.getBaseStation().getAddress(), loadedCityMap);

            if(cityDistrict.getDistrict() != null) {
                filledCityCounter++;
            }
            log.info( "-----------");
        }
        log.info("Record count: {}",  excelRecords.size());
        log.info("filledCityCount: {}", filledCityCounter);
        return excelRecords;
    }*/


    @PostMapping("/record")
    public ResponseEntity<String> recordExcel(@RequestParam String filePath,
            @RequestParam boolean shouldAnalyseHts,
            @RequestParam boolean shouldFilterHtsRecords) {
        excelImportService.importExcel(filePath ,shouldAnalyseHts, shouldFilterHtsRecords);
        return ResponseEntity.ok("SUCCESS");
    }


  /*  @PostMapping("/importExcelWithFreeAPI")
    public ResponseEntity<String> importExcelWithFreeApi(@RequestParam String filePath) {
        excelImportService.importExcelTests(filePath);
        return ResponseEntity.ok("SUCCESS");
    }

    @PostMapping("/checkFreeApi")
    public ResponseEntity<String> checkAddressWithFreeApi(@RequestParam String address) {
        return ResponseEntity.ok(excelImportService.checkFreeApi(address));
    }

    @PostMapping("/checkGeoapifyGeocoderService")
    public ResponseEntity<String> checkAddressWithGeoapifyGeocoderService(@RequestParam String address) {
        return ResponseEntity.ok(excelImportService.checkByGeoapify(address));
    } */

}
