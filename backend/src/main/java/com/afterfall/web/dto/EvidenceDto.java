package com.afterfall.web.dto;

import java.util.List;

public record EvidenceDto(
        String evidenceId,
        long tick,
        String policy,
        String target,
        String originalReport,
        String alteredReport,
        String discrepancyType,
        List<Long> sourceEventIds,
        List<Integer> referencedSurvivorIds,
        boolean isDiscovered
) {}
