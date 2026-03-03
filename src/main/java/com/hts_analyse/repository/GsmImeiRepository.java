package com.hts_analyse.repository;

import com.hts_analyse.entity.GsmImeiEntity;
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
}
