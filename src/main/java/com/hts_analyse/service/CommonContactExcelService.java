package com.hts_analyse.service;

import com.hts_analyse.model.dto.CommonContactDto;
import com.hts_analyse.model.dto.CommonContactMultiShortDto;
import com.hts_analyse.model.dto.CommonContactShortDto;
import com.hts_analyse.model.dto.MutualContactRecordDto;
import com.hts_analyse.model.response.CommonContactMultiResponse;
import com.hts_analyse.model.response.CommonContactResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
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

    public byte[] generateMultiExcel(CommonContactMultiResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            createCommonCommunicationsMultiSheetGrouped(workbook, response.getCommonCommunications(), response.getTotalCommonCount());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel dosyası oluşturulamadı", e);
        }
    }

    public byte[] generateMutualContactsExcel(List<MutualContactRecordDto> records) {
        try (Workbook workbook = new XSSFWorkbook()) {
            createMutualContactsSheetGrouped(workbook, records);

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

    private void createCommonCommunicationsMultiSheetGrouped(Workbook workbook, List<CommonContactMultiShortDto> list, int total) {
        Sheet sheet = workbook.createSheet("Common Communications");
        int rowIdx = 0;

        Row totalRow = sheet.createRow(rowIdx++);
        totalRow.createCell(0).setCellValue("Total Common Count");
        totalRow.createCell(1).setCellValue(total);

        Map<String, List<CommonContactMultiShortDto>> grouped = list.stream()
                .collect(Collectors.groupingBy(CommonContactMultiShortDto::getCommonGsms));

        List<String> groupKeys = grouped.keySet().stream()
                .sorted()
                .toList();

        for (String groupKey : groupKeys) {
            Row groupRow = sheet.createRow(rowIdx++);
            groupRow.createCell(0).setCellValue("Common GSMs = " + groupKey);

            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("GSM");
            header.createCell(1).setCellValue("Identity No");
            header.createCell(2).setCellValue("Full Name");
            header.createCell(3).setCellValue("Total Communication Count");

            for (CommonContactMultiShortDto dto : grouped.get(groupKey)) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getGsm());
                row.createCell(1).setCellValue(dto.getIdentityNo() != null ? dto.getIdentityNo() : "");
                row.createCell(2).setCellValue(dto.getFullName() != null ? dto.getFullName() : "");
                row.createCell(3).setCellValue(dto.getTotalCommunicationCount());
            }
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

    private void createMutualContactsSheetGrouped(Workbook workbook, List<MutualContactRecordDto> records) {
        Sheet sheet = workbook.createSheet("Mutual Contacts");
        int rowIdx = 0;

        Map<String, List<MutualContactRecordDto>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.getGsmNumber() + "_" + r.getOtherNumber()));

        List<String> groupKeys = grouped.keySet().stream()
                .sorted()
                .toList();

        for (String groupKey : groupKeys) {
            Row groupRow = sheet.createRow(rowIdx++);
            groupRow.createCell(0).setCellValue("Common GSMs = " + groupKey);

            Row summaryHeader = sheet.createRow(rowIdx++);
            summaryHeader.createCell(0).setCellValue("Tip");
            summaryHeader.createCell(1).setCellValue("Toplam");

            Map<String, Long> typeCounts = grouped.get(groupKey).stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getRecordType() != null ? r.getRecordType() : "",
                            Collectors.counting()
                    ));

            List<Map.Entry<String, Long>> summaryRows = typeCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .toList();

            for (Map.Entry<String, Long> e : summaryRows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue());
            }

            Row spacer = sheet.createRow(rowIdx++);
            spacer.createCell(0).setCellValue("");

            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Numara");
            header.createCell(1).setCellValue("Tip");
            header.createCell(2).setCellValue("Diğer Numara");
            header.createCell(3).setCellValue("Tarih");
            header.createCell(4).setCellValue("Baz Bilgisi");

            List<MutualContactRecordDto> sorted = grouped.get(groupKey).stream()
                    .sorted(Comparator.comparing(MutualContactRecordDto::getRecordTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            for (MutualContactRecordDto r : sorted) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getGsmNumber());
                row.createCell(1).setCellValue(r.getRecordType() != null ? r.getRecordType() : "");
                row.createCell(2).setCellValue(r.getOtherNumber());
                row.createCell(3).setCellValue(formatDateTime(r.getRecordTime()));
                row.createCell(4).setCellValue(r.getBaseStationInfo() != null ? r.getBaseStationInfo() : "");
            }
        }
    }

    private String pairKey(String a, String b) {
        if (a == null || b == null) return "";
        if (a.compareTo(b) <= 0) {
            return a + "_" + b;
        }
        return b + "_" + a;
    }

    private String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return dt.format(f);
    }

    private String mapToString(Map<String, Long> map) {
        if (map == null || map.isEmpty()) return "";
        return map.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
