package com.hts_analyse.service;

import com.hts_analyse.model.dto.BaseStationDto;
import com.hts_analyse.model.dto.ExcelRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelReaderService {

    public List<ExcelRecord> readExcel(String filePath) {
        List<ExcelRecord> records = new ArrayList<>();

        IOUtils.setByteArrayMaxOverride(200_000_000);

        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;

            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false; // skip header
                    continue;
                }

                String orderNo = row.getCell(0).getStringCellValue();
                String gsmNumber = getCellValue(row.getCell(1));
                String recordType = getCellValue(row.getCell(2));
                String otherNumber = getCellValue(row.getCell(3));
                String recordTime = getCellValue(row.getCell(4));
                String time = getCellValue(row.getCell(5));
                String fullName = getCellValue(row.getCell(6));
                String identityNo = getCellValue(row.getCell(7));
                String imei = getCellValue(row.getCell(8));
                String baseStationRaw = getCellValue(row.getCell(9));

                BaseStationDto baseStation = parseBaseStation(baseStationRaw);
                if(Objects.isNull(baseStation)) {
                    continue;
                }

                ExcelRecord excelRecord = ExcelRecord.builder()
                        .gsmNumber(gsmNumber)
                        .recordType(recordType)
                        .otherNumber(otherNumber)
                        .recordTime(recordTime)
                        .fullName(fullName)
                        .baseStation(baseStation)
                        .identityNo(identityNo)
                        .imei(imei)
                        .build();

                records.add(excelRecord);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return records;
    }

    public List<ExcelRecord> readFullExcel(String filePath) {
        List<ExcelRecord> records = new ArrayList<>();
        IOUtils.setByteArrayMaxOverride(200_000_000);

        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean insideRelevantSection = false;
            Map<String, Integer> columnIndexMap = new HashMap<>();

            for (Row row : sheet) {
                Cell firstCellObj = row.getCell(0);
                if (firstCellObj == null) continue;

                String firstCell = getCellValue(firstCellObj).trim().toUpperCase();
                if (firstCell.equals("GSM GÖRÜŞME SORGU SONUÇLARI") ||
                        firstCell.equals("İNTERNET BAĞLANTI (GPRS) İLETİŞİM SORGU SONUÇLARI")) {
                    insideRelevantSection = true;
                    columnIndexMap.clear(); // yeni tablo için reset
                    continue;
                }

                if (firstCell.endsWith("SORGU SONUÇLARI")) {
                    insideRelevantSection = false;
                    continue;
                }

                if (!insideRelevantSection) continue;

                if (columnIndexMap.isEmpty()) {
                    for (Cell cell : row) {
                        String header = getCellValue(cell).trim().toUpperCase();
                        columnIndexMap.put(header, cell.getColumnIndex());
                    }
                    continue;
                }

                try {
                    ExcelRecord excelRecord = ExcelRecord.builder()
                            .gsmNumber(getValue(columnIndexMap, row, "NUMARA"))
                            .recordType(getValue(columnIndexMap, row, "TİP"))
                            .otherNumber(getValue(columnIndexMap, row, "DİĞER NUMARA"))
                            .recordTime(getValue(columnIndexMap, row, "TARİH"))
                            .fullName(getValue(columnIndexMap, row, "İSİM SOYİSİM ( DİĞER NUMARA)"))
                            .identityNo(getValue(columnIndexMap, row, "TC KİMLİK NO (DİĞER NUMARA)"))
                            .imei(getValue(columnIndexMap, row, "IMEI"))
                            .baseStation(parseBaseStation(getValue(columnIndexMap, row, "BAZ (NUMARA)")))
                            .build();

                    records.add(excelRecord);
                } catch (Exception e) {
                    log.warn("Satır okunamadı, atlanıyor. Hata: {}", e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return records;
    }

    private String getValue(Map<String, Integer> map, Row row, String key) {
        Integer idx = map.get(key);
        if (idx == null) return "";
        return getCellValue(row.getCell(idx));
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private BaseStationDto parseBaseStation(String raw) {
        if (raw == null || raw.isBlank()) {
            return new BaseStationDto("", "", "");
        }

        try{
            String[] parts = raw.split(" - ");
            String id = parts.length > 0 ? parts[0].trim() : "";
            String operator = parts.length > 1 ? parts[1].trim() : "";
            String address = parts.length > 2 ? parts[2].trim() : "";

            return BaseStationDto.builder()
                    .baseStationId(id)
                    .operator(operator)
                    .address(address)
                    .build();
        }catch (Exception e){
            log.error("parse edilemeyen string: " + raw);
            return null;
        }
    }
}
