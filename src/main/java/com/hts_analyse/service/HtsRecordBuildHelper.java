package com.hts_analyse.service;

import com.hts_analyse.cache.BaseStationInfoCache;
import com.hts_analyse.entity.BaseStationInfoEntity;
import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.model.dto.BaseStationDto;
import com.hts_analyse.model.dto.ExcelRecord;
import com.hts_analyse.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HtsRecordBuildHelper {

    private final BaseStationInfoCache baseStationInfoCache;

    public HtsRecordEntity buildHtsRecordEntity(ExcelRecord excelRecord, boolean shouldAnalyseHts) {
        if (excelRecord == null) return null;

        BaseStationDto baseStationDto = excelRecord.getBaseStation();

        if (shouldAnalyseHts && !hasRequiredStationFields(baseStationDto)) {
            return null;
        }

        String baseStationId = extractBaseStationId(baseStationDto);
        String operator = extractOperator(baseStationDto);
        BaseStationInfoEntity stationInfo = resolveStationInfo(shouldAnalyseHts, baseStationId);

        CoordinateResult coordinates = resolveCoordinates(shouldAnalyseHts, baseStationDto, stationInfo);
        if (coordinates == null) return null;

        return HtsRecordEntity.builder()
                .gsmNumber(excelRecord.getGsmNumber())
                .recordType(excelRecord.getRecordType())
                .otherNumber(excelRecord.getOtherNumber())
                .recordDatetime(DateUtil.convertStringToLocalDateTime(excelRecord.getRecordTime()))
                .fullName(excelRecord.getFullName())
                .baseStationId(baseStationId)
                .operator(operator)
                .address(resolveAddress(shouldAnalyseHts, stationInfo, baseStationDto))
                .city(resolveCity(shouldAnalyseHts, stationInfo))
                .district(resolveDistrict(shouldAnalyseHts, stationInfo))
                .latitude(coordinates.latitude)
                .longitude(coordinates.longitude)
                .imei(excelRecord.getImei())
                .identityNo(excelRecord.getIdentityNo())
                .build();
    }

    private boolean hasRequiredStationFields(BaseStationDto baseStationDto) {
        if (baseStationDto == null) return false;
        return StringUtils.isNotBlank(baseStationDto.getBaseStationId())
                && StringUtils.isNotBlank(baseStationDto.getAddress());
    }

    private BaseStationInfoEntity resolveStationInfo(boolean shouldAnalyseHts, String baseStationId) {
        if (!shouldAnalyseHts) return null;
        if (StringUtils.isBlank(baseStationId)) return null;
        return baseStationInfoCache.get(baseStationId);
    }

    private String extractBaseStationId(BaseStationDto baseStationDto) {
        return baseStationDto != null ? baseStationDto.getBaseStationId() : null;
    }

    private String extractOperator(BaseStationDto baseStationDto) {
        return baseStationDto != null ? baseStationDto.getOperator() : null;
    }

    private CoordinateResult resolveCoordinates(boolean shouldAnalyseHts, BaseStationDto baseStationDto, BaseStationInfoEntity stationInfo) {
        if (!shouldAnalyseHts) {
            return new CoordinateResult(null, null);
        }

        boolean hasExcelCoordinates = hasCoordinates(baseStationDto);

        if (!hasExcelCoordinates && stationInfo == null) {
            return null;
        }

        if (hasExcelCoordinates) {
            return new CoordinateResult(baseStationDto.getLatitude(), baseStationDto.getLongitude());
        }

        return new CoordinateResult(stationInfo.getLatitude(), stationInfo.getLongitude());
    }

    private String resolveAddress(boolean shouldAnalyseHts, BaseStationInfoEntity stationInfo, BaseStationDto baseStationDto) {
        if (!shouldAnalyseHts) return "";

        if (stationInfo != null) {
            return stationInfo.getAddress();
        }

        return baseStationDto != null ? baseStationDto.getAddress() : "";
    }

    private String resolveCity(boolean shouldAnalyseHts, BaseStationInfoEntity stationInfo) {
        return shouldAnalyseHts && stationInfo != null ? stationInfo.getCity() : "";
    }

    private String resolveDistrict(boolean shouldAnalyseHts, BaseStationInfoEntity stationInfo) {
        return shouldAnalyseHts && stationInfo != null ? stationInfo.getDistrict() : "";
    }

    private boolean hasCoordinates(BaseStationDto baseStationDto) {
        return baseStationDto != null
                && baseStationDto.getLatitude() != null
                && baseStationDto.getLongitude() != null;
    }

    private record CoordinateResult(Double latitude, Double longitude) {}
}