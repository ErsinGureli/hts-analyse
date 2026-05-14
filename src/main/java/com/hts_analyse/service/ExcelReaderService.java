package com.hts_analyse.service;

import com.hts_analyse.model.dto.BaseStationDto;
import com.hts_analyse.model.dto.ExcelRecord;
import com.hts_analyse.model.dto.FullExcelRecord;
import com.hts_analyse.model.dto.GsmImeiDto;
import com.hts_analyse.model.dto.SubscriptionInformationRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ExcelReaderService {

    private static final String GSM_SECTION = "GSM GÖRÜŞME SORGU SONUÇLARI";
    private static final String GPRS_SECTION = "İNTERNET BAĞLANTI (GPRS) İLETİŞİM SORGU SONUÇLARI";
    private static final String SUBSCRIPTION_INFORMATION_SECTION = "ABONE BİLGİLERİ";

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

                String orderNo = getCellValue(row.getCell(0)); // kullanılmıyorsa silinebilir
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
                if (baseStation == null) {
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
        return readFullExcelData(filePath).getHtsRecords();
    }

    public FullExcelRecord readFullExcelData(String filePath) {
        List<ExcelRecord> records = new ArrayList<>();
        List<SubscriptionInformationRecord> subscriptionInformations = new ArrayList<>();
        IOUtils.setByteArrayMaxOverride(200_000_000);

        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean insideRelevantSection = false;
            boolean insideSubscriptionInformationSection = false;
            Map<String, Integer> htsColumnIndexMap = new HashMap<>();
            Map<String, Integer> subscriptionColumnIndexMap = new HashMap<>();

            for (Row row : sheet) {
                Cell firstCellObj = row.getCell(0);
                if (firstCellObj == null) continue;

                String firstCell = getCellValue(firstCellObj).trim().toUpperCase();

                if (firstCell.equals(GSM_SECTION) || firstCell.equals(GPRS_SECTION)) {
                    insideRelevantSection = true;
                    insideSubscriptionInformationSection = false;
                    htsColumnIndexMap.clear();
                    continue;
                }

                if (firstCell.equals(SUBSCRIPTION_INFORMATION_SECTION)) {
                    insideSubscriptionInformationSection = true;
                    insideRelevantSection = false;
                    subscriptionColumnIndexMap.clear();
                    continue;
                }

                if (firstCell.endsWith("SORGU SONUÇLARI")) {
                    insideRelevantSection = false;
                    insideSubscriptionInformationSection = false;
                    continue;
                }

                if (insideSubscriptionInformationSection) {
                    if (subscriptionColumnIndexMap.isEmpty()) {
                        for (Cell cell : row) {
                            String header = getCellValue(cell).trim();
                            subscriptionColumnIndexMap.put(header, cell.getColumnIndex());
                        }
                        continue;
                    }

                    try {
                        if (isRowEmpty(row)) continue;

                        SubscriptionInformationRecord record = SubscriptionInformationRecord.builder()
                                .orderNo(getValue(subscriptionColumnIndexMap, row, "SIRA NO"))
                                .gsmNumber(getValue(subscriptionColumnIndexMap, row, "NUMARA"))
                                .status(getValue(subscriptionColumnIndexMap, row, "DURUM"))
                                .firstName(getValue(subscriptionColumnIndexMap, row, "AD"))
                                .lastName(getValue(subscriptionColumnIndexMap, row, "SOYAD"))
                                .address(getValue(subscriptionColumnIndexMap, row, "ADRES"))
                                .birthDate(getValue(subscriptionColumnIndexMap, row, "DOGUM TARİHİ"))
                                .birthPlace(getValue(subscriptionColumnIndexMap, row, "DOGUM YERİ"))
                                .district(getValue(subscriptionColumnIndexMap, row, "İLÇE"))
                                .city(getValue(subscriptionColumnIndexMap, row, "İL"))
                                .identityNo(getValue(subscriptionColumnIndexMap, row, "TC KİMLİK NO"))
                                .motherName(getValue(subscriptionColumnIndexMap, row, "ANNE ADI"))
                                .fatherName(getValue(subscriptionColumnIndexMap, row, "BABA ADI"))
                                .subscriptionQueryRange(getValue(subscriptionColumnIndexMap, row, "ABONE SORGU ARALIĞI"))
                                .subscriptionStartDate(getValue(subscriptionColumnIndexMap, row, "ABONE BASLANGIÇ"))
                                .subscriptionEndDate(getValue(subscriptionColumnIndexMap, row, "ABONE BİTİŞ"))
                                .operator(getValue(subscriptionColumnIndexMap, row, "OPERATÖR"))
                                .build();

                        if (record.getGsmNumber().isBlank()) continue;

                        subscriptionInformations.add(record);
                    } catch (Exception e) {
                        log.warn("Abone bilgisi satırı okunamadı, atlanıyor. Hata: {}", e.getMessage());
                    }

                    continue;
                }

                if (!insideRelevantSection) continue;

                if (htsColumnIndexMap.isEmpty()) {
                    for (Cell cell : row) {
                        String header = getCellValue(cell).trim();
                        htsColumnIndexMap.put(header, cell.getColumnIndex());
                    }
                    continue;
                }

                try {
                    if (isRowEmpty(row)) continue;

                    ExcelRecord excelRecord = ExcelRecord.builder()
                            .gsmNumber(getValue(htsColumnIndexMap, row, "NUMARA"))
                            .recordType(getValue(htsColumnIndexMap, row, "TİP"))
                            .otherNumber(getValue(htsColumnIndexMap, row, "DİĞER NUMARA"))
                            .recordTime(getValue(htsColumnIndexMap, row, "TARİH"))
                            .fullName(getValue(htsColumnIndexMap, row, "İSİM SOYİSİM ( DİĞER NUMARA)"))
                            .identityNo(getValue(htsColumnIndexMap, row, "TC KİMLİK NO (DİĞER NUMARA)"))
                            .imei(getValue(htsColumnIndexMap, row, "IMEI"))
                            .baseStation(parseBaseStation(getValue(htsColumnIndexMap, row, "BAZ (NUMARA)")))
                            .build();

                    records.add(excelRecord);
                } catch (Exception e) {
                    log.warn("Satır okunamadı, atlanıyor. Hata: {}", e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return FullExcelRecord.builder()
                .htsRecords(records)
                .subscriptionInformations(subscriptionInformations)
                .build();
    }

    // ----------------------------------------------------
// SADE GSM + IMEI OKUMA
// ----------------------------------------------------

    public List<GsmImeiDto> readGsmImeiOnly(String filePath) {

        List<GsmImeiDto> records = new ArrayList<>();
        IOUtils.setByteArrayMaxOverride(200_000_000);

        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Integer> columnIndexMap = new HashMap<>();
            boolean headerFound = false;

            for (Row row : sheet) {

                if (!headerFound) {

                    Map<String, Integer> tempMap = new HashMap<>();

                    for (Cell cell : row) {
                        String rawHeader = getCellValue(cell).trim();
                        String normalized = normalize(rawHeader);
                        tempMap.put(normalized, cell.getColumnIndex());
                    }

                    // HEADER doğrulaması
                    if (tempMap.containsKey("SIRA NO")
                            && tempMap.containsKey("NUMARA")
                            && tempMap.containsKey("IMEI")) {

                        columnIndexMap = tempMap;
                        headerFound = true;
                    }

                    continue;
                }

                try {
                    Integer gsmIdx = columnIndexMap.get("NUMARA");
                    Integer imeiIdx = columnIndexMap.get("IMEI");

                    if (gsmIdx == null || imeiIdx == null) continue;

                    String gsm = getCellValue(row.getCell(gsmIdx)).trim();
                    String imei = getCellValue(row.getCell(imeiIdx)).trim();

                    // GSM boşsa skip
                    if (gsm.isBlank()) continue;

                    // IMEI 14/15 hane kontrolü
                    if (!imei.matches("\\d{14,15}")) continue;

                    records.add(
                            GsmImeiDto.builder()
                                    .gsm(gsm)
                                    .imei(imei)
                                    .build()
                    );

                } catch (Exception e) {
                    log.warn("Satır okunamadı, atlanıyor. Hata: {}", e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return records;
    }

    // ----------------------------------------------------
    // HEADER EŞLEŞTİRME
    // ----------------------------------------------------

    private String getValue(Map<String, Integer> map, Row row, String expectedHeader) {
        Integer idx = findColumnIndex(map, expectedHeader);
        if (idx == null) {
            return "";
        }
        return getCellValue(row.getCell(idx));
    }

    private Integer findColumnIndex(Map<String, Integer> columnIndexMap, String expectedHeader) {
        String normalizedExpected = normalize(expectedHeader);

        for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
            String header = entry.getKey();
            String normalizedHeader = normalize(header);

            if (normalizedHeader.equals(normalizedExpected)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (Cell cell : row) {
            if (!getCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tüm Türkçe karakterleri ASCII'ye çevirir,
     * gereksiz noktalama işaretlerini atar, boşlukları sadeleştirir.
     */
    private String normalize(String s) {
        if (s == null) return "";

        String upper = s.toUpperCase(Locale.ROOT);

        // Türkçe karakterleri ASCII'ye indir
        upper = upper
                .replace('İ', 'I')
                .replace('I', 'I') // no-op ama dursun
                .replace('I', 'I')
                .replace('Ğ', 'G')
                .replace('Ü', 'U')
                .replace('Ş', 'S')
                .replace('Ö', 'O')
                .replace('Ç', 'C')
                // olası küçükler kalırsa
                .replace('ı', 'I')
                .replace('ğ', 'G')
                .replace('ü', 'U')
                .replace('ş', 'S')
                .replace('ö', 'O')
                .replace('ç', 'C');

        // Harf/rakam dışındaki her şeyi boşluk yap
        upper = upper.replaceAll("[^A-Z0-9]+", " ");
        // Birden fazla boşluğu teke indir
        upper = upper.replaceAll("\\s+", " ").trim();

        return upper;
    }

    // ----------------------------------------------------
    // CELL OKUMA
    // ----------------------------------------------------

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

    // ----------------------------------------------------
    // BAZ İSTASYONU PARSE
    // ----------------------------------------------------

    private BaseStationDto parseBaseStation(String raw) {
        if (raw == null || raw.isBlank()) {
            return BaseStationDto.builder().build();
        }

        String cleaned = raw.trim();

        Double latitude = null;
        Double longitude = null;

        // ---- LAT / LON YAKALA (sondan) ----
        // Örnek: ", 41.0392- 28.5485"
        Pattern latLonPattern = Pattern.compile(
                ",\\s*([0-9]+\\.[0-9]+)\\s*-\\s*([0-9]+\\.[0-9]+)\\s*$"
        );

        Matcher matcher = latLonPattern.matcher(cleaned);
        if (matcher.find()) {
            latitude = Double.valueOf(matcher.group(1));
            longitude = Double.valueOf(matcher.group(2));

            // lat-lon kısmını stringden düş
            cleaned = cleaned.substring(0, matcher.start()).trim();
        }

        // ---- KALAN KISMI PARSE ET ----
        String[] parts = cleaned.split(" - ", 3);

        String id = parts.length > 0 ? parts[0].trim() : "";
        String operator = parts.length > 1 ? parts[1].trim() : "";
        String address = parts.length > 2 ? parts[2].trim() : "";

        return BaseStationDto.builder()
                .baseStationId(id)
                .operator(operator)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
