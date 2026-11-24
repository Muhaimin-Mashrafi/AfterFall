package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.InterventionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataInterventionRecordRepository extends JpaRepository<InterventionRecordEntity, InterventionRecordEntity.InterventionCompositeKey> {
    List<InterventionRecordEntity> findByRunIdOrderByTickAsc(String runId);
}
