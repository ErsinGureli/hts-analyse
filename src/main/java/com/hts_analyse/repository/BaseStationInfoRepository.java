package com.hts_analyse.repository;

import com.hts_analyse.entity.BaseStationInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaseStationInfoRepository extends JpaRepository<BaseStationInfoEntity, Long> {

    Optional<BaseStationInfoEntity> findByBaseStationId(String baseStationId);
}