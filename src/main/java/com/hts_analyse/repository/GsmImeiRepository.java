package com.hts_analyse.repository;

import com.hts_analyse.entity.GsmImeiEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GsmImeiRepository extends JpaRepository<GsmImeiEntity, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO gsm_imei (gsm, imei)
        VALUES (:gsm, :imei)
        ON CONFLICT (gsm, imei) DO NOTHING
        """, nativeQuery = true)
    int insertIgnore(@Param("gsm") String gsm, @Param("imei") String imei);

    @Query(value = """
        SELECT imei, gsm
        FROM gsm_imei
        WHERE gsm IN (:gsmNumbers)
        GROUP BY imei, gsm
        """, nativeQuery = true)
    List<Object[]> findDistinctGsmNumbersByImei(@Param("gsmNumbers") List<String> gsmNumbers);

    @Query(value = """
        SELECT imei, gsm
        FROM gsm_imei
        WHERE gsm IN (:gsmNumbers)
          AND created_at BETWEEN :startTime AND :endTime
        GROUP BY imei, gsm
        """, nativeQuery = true)
    List<Object[]> findDistinctGsmNumbersByImeiAndCreatedAtBetween(
            @Param("gsmNumbers") List<String> gsmNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
