package com.hts_analyse.service;

import com.hts_analyse.model.dto.ImeiSharedGsmDto;
import com.hts_analyse.model.response.ImeiSharedGsmResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ImeiExcelService {

    public byte[] generateCommonImeisExcel(ImeiSharedGsmResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            createCommonImeisSheet(workbook, response.getSharedImeis(), response.getTotalImeiCount());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel dosyası oluşturulamadı", e);
        }
    }

    private void createCommonImeisSheet(Workbook workbook, List<ImeiSharedGsmDto> list, int total) {
        Sheet sheet = workbook.createSheet("Common IMEIs");
        int rowIdx = 0;

        Row totalRow = sheet.createRow(rowIdx++);
        totalRow.createCell(0).setCellValue("Total IMEI Count");
        totalRow.createCell(1).setCellValue(total);

        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("IMEI");
        header.createCell(1).setCellValue("GSM Count");
        header.createCell(2).setCellValue("GSM Numbers");

        for (ImeiSharedGsmDto dto : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getImei());
            row.createCell(1).setCellValue(dto.getGsmCount());
            row.createCell(2).setCellValue(String.join(", ", dto.getGsmNumbers()));
        }
    }
}
