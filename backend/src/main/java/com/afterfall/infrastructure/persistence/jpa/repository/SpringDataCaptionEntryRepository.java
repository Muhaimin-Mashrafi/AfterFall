package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.CaptionEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCaptionEntryRepository extends JpaRepository<CaptionEntryEntity, CaptionEntryEntity.CaptionCompositeKey> {
    List<CaptionEntryEntity> findByRunIdOrderByTickAsc(String runId);
}
