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
    List<HtsRecordEntity> findAllByGsmNumber(String string);

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
}
