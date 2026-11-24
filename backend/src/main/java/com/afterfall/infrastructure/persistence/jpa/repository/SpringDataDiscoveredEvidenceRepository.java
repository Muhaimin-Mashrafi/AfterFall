package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.DiscoveredEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDiscoveredEvidenceRepository extends JpaRepository<DiscoveredEvidenceEntity, Long> {

    List<DiscoveredEvidenceEntity> findByRunIdOrderByTickAsc(String runId);

    Optional<DiscoveredEvidenceEntity> findByRunIdAndEvidenceId(String runId, String evidenceId);

    List<DiscoveredEvidenceEntity> findByRunIdAndIsDiscoveredTrueOrderByTickAsc(String runId);
}
