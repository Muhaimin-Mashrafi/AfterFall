package com.afterfall.web.dto;

public record InterventionRecordDto(
        String id,
        long tick,
        String target,
        String type,
        String originalReport,
        String alteredReport,
        String status
) {}
