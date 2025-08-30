package com.hts_analyse.mapper;

import com.hts_analyse.entity.HtsRecordEntity;

import com.hts_analyse.model.dto.HtsRecordDto;
import java.util.Collections;
import java.util.List;

public final class HtsRecordMapper {

    private HtsRecordMapper() {}

    public static HtsRecordDto toDto(HtsRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        return HtsRecordDto.builder()
                .id(entity.getId())
                .gsmNumber(entity.getGsmNumber())
                .recordType(entity.getRecordType())
                .otherNumber(entity.getOtherNumber())
                .recordDatetime(entity.getRecordDatetime())
                .fullName(entity.getFullName())
                .baseStationId(entity.getBaseStationId())
                .operator(entity.getOperator())
                .address(entity.getAddress())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .identityNo(entity.getIdentityNo())
                .imei(entity.getImei())
                .build();
    }

    public static List<HtsRecordDto> toDtoList(List<HtsRecordEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(HtsRecordMapper::toDto)
                .toList();
    }
}
