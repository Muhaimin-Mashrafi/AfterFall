package com.afterfall.infrastructure.persistence.inmemory;

import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.RunId;
import com.afterfall.recap.EvidenceItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of {@link EvidenceDiscoveryRepository}.
 */
public class InMemoryEvidenceDiscoveryRepository implements EvidenceDiscoveryRepository {

    private final Map<RunId, Map<String, EvidenceItem>> availableEvidence = new ConcurrentHashMap<>();
    private final Map<RunId, Set<String>> discoveredEvidence = new ConcurrentHashMap<>();

    @Override
    public void saveAvailableEvidence(RunId runId, EvidenceItem evidence) {
        if (runId == null || evidence == null) return;
        availableEvidence.computeIfAbsent(runId, r -> new LinkedHashMap<>()).put(evidence.evidenceId(), evidence);
    }

    @Override
    public void saveAllAvailableEvidence(RunId runId, List<EvidenceItem> evidenceList) {
        if (runId == null || evidenceList == null) return;
        for (EvidenceItem item : evidenceList) {
            saveAvailableEvidence(runId, item);
        }
    }

    @Override
    public List<EvidenceItem> availableEvidenceForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        Map<String, EvidenceItem> map = availableEvidence.get(runId);
        if (map == null) return Collections.emptyList();
        return new ArrayList<>(map.values());
    }

    @Override
    public Optional<EvidenceItem> findEvidence(RunId runId, String evidenceId) {
        if (runId == null || evidenceId == null) return Optional.empty();
        Map<String, EvidenceItem> map = availableEvidence.get(runId);
        if (map == null) return Optional.empty();
        return Optional.ofNullable(map.get(evidenceId));
    }

    @Override
    public synchronized boolean markDiscovered(RunId runId, String evidenceId) {
        if (runId == null || evidenceId == null) return false;
        Set<String> set = discoveredEvidence.computeIfAbsent(runId, r -> Collections.synchronizedSet(new LinkedHashSet<>()));
        return set.add(evidenceId);
    }

    @Override
    public boolean isDiscovered(RunId runId, String evidenceId) {
        if (runId == null || evidenceId == null) return false;
        Set<String> set = discoveredEvidence.get(runId);
        return set != null && set.contains(evidenceId);
    }

    @Override
    public Set<String> discoveredEvidenceIdsForRun(RunId runId) {
        if (runId == null) return Collections.emptySet();
        Set<String> set = discoveredEvidence.get(runId);
        if (set == null) return Collections.emptySet();
        return new LinkedHashSet<>(set);
    }

    @Override
    public List<EvidenceItem> discoveredEvidenceForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        Set<String> ids = discoveredEvidenceIdsForRun(runId);
        Map<String, EvidenceItem> map = availableEvidence.get(runId);
        if (map == null || ids.isEmpty()) return Collections.emptyList();

        List<EvidenceItem> result = new ArrayList<>();
        for (String id : ids) {
            EvidenceItem item = map.get(id);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
}
