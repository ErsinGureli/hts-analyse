package com.hts_analyse.controller;

import com.hts_analyse.model.dto.GsmImeiDto;
import com.hts_analyse.service.ExcelImportService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @PostMapping("/record")
    public ResponseEntity<String> recordExcel(@RequestParam String filePath,
            @RequestParam boolean shouldAnalyseHts,
            @RequestParam boolean shouldFilterHtsRecords) {
        excelImportService.importExcel(filePath ,shouldAnalyseHts, shouldFilterHtsRecords);
        return ResponseEntity.ok("SUCCESS");
    }

    @PostMapping("/record-imei")
    public ResponseEntity<String> recordImeiExcel(@RequestParam String filePath) {
        excelImportService.importGsmImei(filePath);
        return ResponseEntity.ok("SUCCESS");
    }

    @PostMapping("/record-imei-folder")
    public ResponseEntity<Map<String, Integer>> recordImeiFolder(@RequestParam String folderPath) {
        Map<String, Integer> fileToCount = new LinkedHashMap<>();
        List<Path> files = listExcelFiles(folderPath);
        for (Path path : files) {
            List<GsmImeiDto> records = excelImportService.importGsmImei(path.toString());
            fileToCount.put(path.getFileName().toString(), records.size());
        }

        return ResponseEntity.ok(fileToCount);
    }

    @PostMapping("/record-folder")
    public ResponseEntity<Map<String, String>> recordExcelFolder(
            @RequestParam String folderPath,
            @RequestParam boolean shouldAnalyseHts,
            @RequestParam boolean shouldFilterHtsRecords) {
        Map<String, String> fileToStatus = new LinkedHashMap<>();
        List<Path> files = listExcelFiles(folderPath);

        for (Path path : files) {
            String fileName = path.getFileName().toString();
            try {
                excelImportService.importExcel(path.toString(), shouldAnalyseHts, shouldFilterHtsRecords);
                fileToStatus.put(fileName, "SUCCESS");
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.isBlank()) {
                    message = e.getClass().getSimpleName();
                }
                if (message.length() > 120) {
                    message = message.substring(0, 120) + "...";
                }
                fileToStatus.put(fileName, "FAILED: " + message);
            }
        }

        return ResponseEntity.ok(fileToStatus);
    }

    private List<Path> listExcelFiles(String folderPath) {
        Path dir = Path.of(folderPath);

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Invalid directory path: " + folderPath);
        }

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".xls") || name.endsWith(".xlsx");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list directory: " + folderPath, e);
        }
    }

}
