package com.afterfall.web.dto;

public record MetricsResponse(
        long tick,
        String lifecycle,
        int aliveSurvivors,
        int deceasedSurvivors,
        int foodQuantity,
        int waterQuantity,
        int medicineQuantity,
        int totalEventsRecorded,
        long droppedMinorEvents,
        boolean fallbackPersistenceActive
) {}
