package com.hts_analyse.mapper;

import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.model.dto.CommonContactDto;
import com.hts_analyse.model.dto.CommonContactShortDto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HtsAnalyseMapper {

    private HtsAnalyseMapper(){}

    public static CommonContactDto buildDetailedDto(String gsm1, String gsm2, String commonGsm,
                                                    List<HtsRecordEntity> gsm1Records, List<HtsRecordEntity> gsm2Records) {

        long count1 = gsm1Records.size();
        long count2 = gsm2Records.size();

        Map<String, Long> gsm1TypeCounts = countTypes(gsm1Records);
        Map<String, Long> gsm2TypeCounts = countTypes(gsm2Records);

        return CommonContactDto.builder()
                .gsmNumber1(gsm1)
                .gsmNumber2(gsm2)
                .commonGsm(commonGsm)
                .gsm1CommunicationCount(count1)
                .gsm2CommunicationCount(count2)
                .gsm1CommunicationTypeCounts(gsm1TypeCounts)
                .gsm2CommunicationTypeCounts(gsm2TypeCounts)
                .build();
    }

    public static Optional<CommonContactShortDto> buildShortDto(String commonGsm,
                                                                 List<HtsRecordEntity> gsm1Records,
                                                                 List<HtsRecordEntity> gsm2Records) {
        if (commonGsm == null || commonGsm.trim().isEmpty()) return Optional.empty();

        String identityNo = null;
        String fullName = null;

        for (HtsRecordEntity e : Stream.concat(gsm1Records.stream(), gsm2Records.stream()).toList()) {
            if (identityNo == null && e.getIdentityNo() != null) identityNo = e.getIdentityNo();
            if (fullName == null && e.getFullName() != null) fullName = e.getFullName();
            if (identityNo != null && fullName != null) break;
        }

        long totalCount = (long) gsm1Records.size() + gsm2Records.size();

        return Optional.of(CommonContactShortDto.builder()
                .gsm(commonGsm)
                .identityNo(identityNo)
                .fullName(fullName)
                .totalCommunicationCount(totalCount)
                .build());
    }

    private static Map<String, Long> countTypes(List<HtsRecordEntity> records) {
        return records.stream()
                .collect(Collectors.groupingBy(HtsRecordEntity::getRecordType, Collectors.counting()));
    }
}
