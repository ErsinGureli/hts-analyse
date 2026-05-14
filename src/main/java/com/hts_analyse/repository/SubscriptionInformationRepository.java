package com.hts_analyse.repository;

import com.hts_analyse.entity.SubscriptionInformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionInformationRepository extends JpaRepository<SubscriptionInformationEntity, Long> {
}
