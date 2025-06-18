package com.hts_analyse.service;

import com.hts_analyse.model.BaseStationDto;
import com.hts_analyse.model.ExcelRecord;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;

            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false; // skip header
                    continue;
                }

                String gsmNumber = getCellValue(row.getCell(0));
                String recordType = getCellValue(row.getCell(1));
                String otherNumber = ""; //getCellValue(row.getCell(2));
                String recordTime = getCellValue(row.getCell(2));
                String fullName = "";   //getCellValue(row.getCell(4));
                String baseStationRaw = getCellValue(row.getCell(3));

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
                        .build();

                records.add(excelRecord);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return records;
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
