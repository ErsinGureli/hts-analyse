package com.hts_analyse.service;

import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.repository.HtsRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HtsRecordQueryService {

    private final HtsRecordRepository htsRecordRepository;

    public List<HtsRecordEntity> findAllByGsmNumber(String gsmNumber){
        return htsRecordRepository.findAllByGsmNumber(gsmNumber);
    }

    public List<HtsRecordEntity> findNearbyRecords(List<String> gsmNumbers, LocalDateTime startTime, LocalDateTime endTime, double lat, double lon, double distance){
        return htsRecordRepository.findNearbyRecords(gsmNumbers, startTime, endTime, lat, lon, distance);
    }

    public List<HtsRecordEntity> findAllByGsmNumberAndRecordDatetimeBetween(String gsmNumber, LocalDateTime startDate, LocalDateTime endDate){
        return htsRecordRepository.findAllByGsmNumberAndRecordDatetimeBetween(gsmNumber, startDate, endDate);
    }

    public List<HtsRecordEntity> findAllByGsmNumberAndRecordDatetimeAfter(String gsmNumber, LocalDateTime startDate){
        return htsRecordRepository.findAllByGsmNumberAndRecordDatetimeAfter(
                gsmNumber, startDate);
    }

    public List<HtsRecordEntity> findAllByGsmNumberAndRecordDatetimeBefore(String gsmNumber, LocalDateTime endTime){
        return htsRecordRepository.findAllByGsmNumberAndRecordDatetimeBefore(
                gsmNumber, endTime);
    }

    public List<HtsRecordEntity> findAllByGsmNumbersAndOtherNumbers(List<String> gsmNumbers){
        return htsRecordRepository.findAllByGsmNumberInAndOtherNumberIn(gsmNumbers, gsmNumbers);
    }

    public List<HtsRecordEntity> findAllByGsmNumbersAndOtherNumbersBetween(
            List<String> gsmNumbers, LocalDateTime start, LocalDateTime end) {
        return htsRecordRepository.findAllByGsmNumberInAndOtherNumberInAndRecordDatetimeBetween(
                gsmNumbers, gsmNumbers, start, end);
    }

    public List<HtsRecordEntity> findAllByGsmNumbersAndOtherNumbersAfter(
            List<String> gsmNumbers, LocalDateTime start) {
        return htsRecordRepository.findAllByGsmNumberInAndOtherNumberInAndRecordDatetimeAfter(
                gsmNumbers, gsmNumbers, start);
    }

    public List<HtsRecordEntity> findAllByGsmNumbersAndOtherNumbersBefore(
            List<String> gsmNumbers, LocalDateTime end) {
        return htsRecordRepository.findAllByGsmNumberInAndOtherNumberInAndRecordDatetimeBefore(
                gsmNumbers, gsmNumbers, end);
    }

    public List<Object[]> findLastNamesWithCount(@Param("gsmNumber") String gsmNumber){
        return htsRecordRepository.findLastNamesWithCount(gsmNumber);
    }

    public List<Object[]> findFullNameIdentityNoWithCount(String gsmNumber, String lastName){
        return htsRecordRepository.findFullNameIdentityNoWithCount(gsmNumber, lastName);
    }
}
