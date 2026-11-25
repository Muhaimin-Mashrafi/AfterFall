package com.afterfall.infrastructure.persistence.jpa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.infrastructure.persistence.jpa.entity.DiscoveredEvidenceEntity;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataDiscoveredEvidenceRepository;
import com.afterfall.observer.InterventionType;
import com.afterfall.recap.DiscrepancyType;
import com.afterfall.recap.EvidenceItem;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "afterfall.persistence.mode", havingValue = "postgres", matchIfMissing = true)
public class JpaEvidenceDiscoveryRepository implements EvidenceDiscoveryRepository {

    private final SpringDataDiscoveredEvidenceRepository repo;

    public JpaEvidenceDiscoveryRepository(SpringDataDiscoveredEvidenceRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
    }

    @Override
    @Transactional
    public void saveAvailableEvidence(RunId runId, EvidenceItem evidence) {
        if (runId == null || evidence == null) return;
        Optional<DiscoveredEvidenceEntity> existing = repo.findByRunIdAndEvidenceId(runId.value(), evidence.evidenceId());
        if (existing.isPresent()) return;

        String sourceEvents = evidence.sourceEventIds().stream().map(e -> String.valueOf(e.value())).collect(Collectors.joining(","));
        String survivors = evidence.referencedSurvivorIds().stream().map(s -> String.valueOf(s.value())).collect(Collectors.joining(","));

        DiscoveredEvidenceEntity entity = new DiscoveredEvidenceEntity(
                runId.value(),
                evidence.evidenceId(),
                evidence.tick(),
                evidence.policy().name(),
                evidence.target(),
                evidence.originalReport(),
                evidence.alteredReport(),
                evidence.discrepancyType().name(),
                sourceEvents,
                survivors,
                false,
                null
        );
        repo.save(entity);
    }

    @Override
    @Transactional
    public void saveAllAvailableEvidence(RunId runId, List<EvidenceItem> evidenceList) {
        if (runId == null || evidenceList == null) return;
        for (EvidenceItem item : evidenceList) {
            saveAvailableEvidence(runId, item);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceItem> availableEvidenceForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return repo.findByRunIdOrderByTickAsc(runId.value()).stream()
                .map(this::toEvidenceItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceItem> findEvidence(RunId runId, String evidenceId) {
        if (runId == null || evidenceId == null) return Optional.empty();
        return repo.findByRunIdAndEvidenceId(runId.value(), evidenceId).map(this::toEvidenceItem);
    }

    @Override
    @Transactional
    public boolean markDiscovered(RunId runId, String evidenceId) {
        if (runId == null || evidenceId == null) return false;
        Optional<DiscoveredEvidenceEntity> opt = repo.findByRunIdAndEvidenceId(runId.value(), evidenceId);
        if (opt.isPresent()) {
            DiscoveredEvidenceEntity entity = opt.get();
            if (Boolean.TRUE.equals(entity.getIsDiscovered())) {
                return false; // Already discovered
            }
            entity.setIsDiscovered(true);
            entity.setDiscoveredAt(Instant.now());
            repo.save(entity);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDiscovered(RunId runId, String evidenceId) {
        if (runId == null || evidenceId == null) return false;
        return repo.findByRunIdAndEvidenceId(runId.value(), evidenceId)
                .map(DiscoveredEvidenceEntity::getIsDiscovered)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> discoveredEvidenceIdsForRun(RunId runId) {
        if (runId == null) return Collections.emptySet();
        return repo.findByRunIdAndIsDiscoveredTrueOrderByTickAsc(runId.value()).stream()
                .map(DiscoveredEvidenceEntity::getEvidenceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceItem> discoveredEvidenceForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return repo.findByRunIdAndIsDiscoveredTrueOrderByTickAsc(runId.value()).stream()
                .map(this::toEvidenceItem)
                .toList();
    }

    private EvidenceItem toEvidenceItem(DiscoveredEvidenceEntity e) {
        List<EventId> events = Collections.emptyList();
        if (e.getSourceEventIds() != null && !e.getSourceEventIds().isBlank()) {
            events = Arrays.stream(e.getSourceEventIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> new EventId(Long.parseLong(s)))
                    .toList();
        }
        List<SurvivorId> survivors = Collections.emptyList();
        if (e.getReferencedSurvivors() != null && !e.getReferencedSurvivors().isBlank()) {
            survivors = Arrays.stream(e.getReferencedSurvivors().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> new SurvivorId(Integer.parseInt(s)))
                    .toList();
        }

        return new EvidenceItem(
                e.getEvidenceId(),
                e.getTick(),
                InterventionType.valueOf(e.getPolicy()),
                e.getTarget(),
                e.getOriginalReport(),
                e.getAlteredReport(),
                DiscrepancyType.valueOf(e.getDiscrepancyType()),
                events,
                survivors
        );
    }
}
