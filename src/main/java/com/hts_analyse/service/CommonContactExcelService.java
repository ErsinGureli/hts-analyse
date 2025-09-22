package com.hts_analyse.service;

import com.hts_analyse.model.dto.CommonContactDto;
import com.hts_analyse.model.dto.CommonContactShortDto;
import com.hts_analyse.model.response.CommonContactResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
@Service
public class CommonContactExcelService {

    public byte[] generateExcel(CommonContactResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            createCommonCommunicationsSheet(workbook, response.getCommonCommunications(), response.getTotalCommonCount());
            createCommonContactsSheet(workbook, response.getCommonContacts());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel dosyası oluşturulamadı", e);
        }
    }

    private void createCommonCommunicationsSheet(Workbook workbook, List<CommonContactShortDto> list, int total) {
        Sheet sheet = workbook.createSheet("Common Communications");
        int rowIdx = 0;

        Row totalRow = sheet.createRow(rowIdx++);
        totalRow.createCell(0).setCellValue("Total Common Count");
        totalRow.createCell(1).setCellValue(total);

        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("GSM");
        header.createCell(1).setCellValue("Identity No");
        header.createCell(2).setCellValue("Full Name");
        header.createCell(3).setCellValue("Total Communication Count");

        for (CommonContactShortDto dto : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getGsm());
            row.createCell(1).setCellValue(dto.getIdentityNo() != null ? dto.getIdentityNo() : "");
            row.createCell(2).setCellValue(dto.getFullName() != null ? dto.getFullName() : "");
            row.createCell(3).setCellValue(dto.getTotalCommunicationCount());
        }
    }

    private void createCommonContactsSheet(Workbook workbook, List<CommonContactDto> list) {
        Sheet sheet = workbook.createSheet("Common Contacts");
        int rowIdx = 0;

        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("GSM 1");
        header.createCell(1).setCellValue("GSM 2");
        header.createCell(2).setCellValue("Common GSM");
        header.createCell(3).setCellValue("GSM 1 Communication Count");
        header.createCell(4).setCellValue("GSM 2 Communication Count");
        header.createCell(5).setCellValue("GSM 1 Communication Type Counts");
        header.createCell(6).setCellValue("GSM 2 Communication Type Counts");

        for (CommonContactDto dto : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getGsmNumber1());
            row.createCell(1).setCellValue(dto.getGsmNumber2());
            row.createCell(2).setCellValue(dto.getCommonGsm());
            row.createCell(3).setCellValue(dto.getGsm1CommunicationCount());
            row.createCell(4).setCellValue(dto.getGsm2CommunicationCount());
            row.createCell(5).setCellValue(mapToString(dto.getGsm1CommunicationTypeCounts()));
            row.createCell(6).setCellValue(mapToString(dto.getGsm2CommunicationTypeCounts()));
        }
    }

    private String mapToString(Map<String, Long> map) {
        if (map == null || map.isEmpty()) return "";
        return map.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
