package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.recap.EvidenceItem;
import com.afterfall.recap.RunRecap;
import com.afterfall.web.dto.EvidenceDto;
import com.afterfall.web.dto.RecapResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RecapApiController {

    private final SimulationService simulationService;

    public RecapApiController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/recap")
    public ResponseEntity<RecapResponse> getRecap() {
        RunRecap recap = simulationService.getRecap();
        Set<String> discoveredIds = simulationService.getDiscoveredEvidenceIds();

        List<EvidenceDto> evidenceDtos = recap.availableEvidence().stream()
                .map(e -> toEvidenceDto(e, discoveredIds.contains(e.evidenceId())))
                .toList();

        RecapResponse response = new RecapResponse(
                recap.runId(),
                recap.totalTicks(),
                recap.endingChapter(),
                recap.initialPopulation(),
                recap.finalAliveSurvivors(),
                recap.totalDeceasedSurvivors(),
                recap.finalResources(),
                recap.survivorOutcomes(),
                recap.crisisTimeline(),
                recap.choices(),
                recap.interventions(),
                recap.chapterArc(),
                evidenceDtos,
                recap.discoveredEvidenceIds(),
                recap.finalRecording()
        );

        return ResponseEntity.ok(response);
    }

    private EvidenceDto toEvidenceDto(EvidenceItem item, boolean isDiscovered) {
        return new EvidenceDto(
                item.evidenceId(),
                item.tick(),
                item.policy().name(),
                item.target(),
                item.originalReport(),
                item.alteredReport(),
                item.discrepancyType().name(),
                item.sourceEventIds().stream().map(com.afterfall.domain.EventId::value).toList(),
                item.referencedSurvivorIds().stream().map(com.afterfall.domain.SurvivorId::value).toList(),
                isDiscovered
        );
    }
}
