package com.hts_analyse.repository;

import com.hts_analyse.entity.HtsRecordEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HtsRecordRepository extends JpaRepository<HtsRecordEntity, Long> {

    List<HtsRecordEntity> findAllByGsmNumber(String gsmNumber);

    List<HtsRecordEntity> findAllByGsmNumberAndRecordDatetimeBetween(String gsmNumber, LocalDateTime start, LocalDateTime end);

    List<HtsRecordEntity> findAllByGsmNumberAndRecordDatetimeAfter(String gsmNumber, LocalDateTime start);

    List<HtsRecordEntity> findAllByGsmNumberAndRecordDatetimeBefore(String gsmNumber, LocalDateTime end);

    List<HtsRecordEntity> findAllByGsmNumberInAndOtherNumberIn(List<String> gsmNumbers, List<String> otherNumbers);

    List<HtsRecordEntity> findAllByGsmNumberInAndOtherNumberInAndRecordDatetimeBetween(
            List<String> gsmNumbers, List<String> otherNumbers, LocalDateTime start, LocalDateTime end);

    List<HtsRecordEntity> findAllByGsmNumberInAndOtherNumberInAndRecordDatetimeAfter(
            List<String> gsmNumbers, List<String> otherNumbers, LocalDateTime start);

    List<HtsRecordEntity> findAllByGsmNumberInAndOtherNumberInAndRecordDatetimeBefore(
            List<String> gsmNumbers, List<String> otherNumbers, LocalDateTime end);

    @Query(value = """
    SELECT *
    FROM hts_record
    WHERE gsm_number IN (:compareGsmNumbers)
      AND record_time BETWEEN :startTime AND :endTime
      AND calculate_distance(:baseLat, :baseLng, latitude, longitude) <= :distance
""", nativeQuery = true)
    List<HtsRecordEntity> findNearbyRecords(
            @Param("compareGsmNumbers") List<String> compareGsmNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("baseLat") double baseLat,
            @Param("baseLng") double baseLng,
            @Param("distance") double distance
    );

    @Query(value = """

        WITH base AS (
           SELECT
               TRIM(regexp_replace(full_name, '.*\\s+', '')) AS last_name, \s
               full_name
           FROM hts_record
           WHERE gsm_number = :gsmNumber
       )
       SELECT
           last_name,
           COUNT(*)                AS call_count,
           COUNT(DISTINCT full_name) AS person_count
       FROM base
       WHERE last_name <> '' AND last_name IS NOT NULL
       GROUP BY last_name
       ORDER BY person_count DESC;
       """, nativeQuery = true)
    List<Object[]> findLastNamesWithCount(@Param("gsmNumber") String gsmNumber);

    @Query(value = """
        SELECT full_name, identity_no, COUNT(*) AS count
        FROM hts_record
        WHERE gsm_number = :gsmNumber
          AND full_name LIKE CONCAT('%', :lastName)
        GROUP BY full_name, identity_no
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> findFullNameIdentityNoWithCount(
            @Param("gsmNumber") String gsmNumber,
            @Param("lastName") String lastName
    );

    @Query(value = """
        SELECT DISTINCT imei
        FROM hts_record
        WHERE gsm_number = :gsmNumber
          AND imei IS NOT NULL
          AND imei <> ''
        """, nativeQuery = true)
    List<String> findDistinctImeisByGsmNumber(@Param("gsmNumber") String gsmNumber);

    @Query(value = """
        SELECT imei, gsm_number
        FROM hts_record
        WHERE gsm_number IN (:gsmNumbers)
          AND imei IS NOT NULL
          AND imei <> ''
        GROUP BY imei, gsm_number
        """, nativeQuery = true)
    List<Object[]> findDistinctGsmNumbersByImei(@Param("gsmNumbers") List<String> gsmNumbers);

    @Query(value = """
        SELECT imei, gsm_number
        FROM hts_record
        WHERE gsm_number IN (:gsmNumbers)
          AND record_time BETWEEN :startTime AND :endTime
          AND imei IS NOT NULL
          AND imei <> ''
        GROUP BY imei, gsm_number
        """, nativeQuery = true)
    List<Object[]> findDistinctGsmNumbersByImeiAndRecordTimeBetween(
            @Param("gsmNumbers") List<String> gsmNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
