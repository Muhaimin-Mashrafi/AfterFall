package com.afterfall.web.dto;

import java.util.Map;

public record StateResponse(
        String runId,
        long tick,
        String lifecycle,
        String chapter,
        int aliveSurvivors,
        int deceasedSurvivors,
        Map<String, Integer> resources,
        boolean hasPendingChoice,
        boolean hasPendingPause
) {}
