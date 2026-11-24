package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.ChoiceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataChoiceRecordRepository extends JpaRepository<ChoiceRecordEntity, ChoiceRecordEntity.ChoiceCompositeKey> {
    List<ChoiceRecordEntity> findByRunIdOrderByTickAsc(String runId);
}
