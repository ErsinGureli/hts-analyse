package com.hts_analyse.service;

import com.hts_analyse.entity.HtsRecordEntity;
import com.hts_analyse.repository.HtsRecordRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HtsBatchWriter {

    private final HtsRecordRepository htsRecordRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<HtsRecordEntity> htsRecordEntities) {
        htsRecordRepository.saveAll(htsRecordEntities);
        entityManager.flush();
        entityManager.clear();
    }
}
