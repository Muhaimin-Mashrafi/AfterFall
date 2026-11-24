package com.afterfall.web.dto;

import java.util.List;

public record RecordsResponse(
        List<InterventionAuditDto> interventions
) {
    public record InterventionAuditDto(
            long id,
            long tick,
            String type,
            String target,
            String reason,
            String originalReport,
            String alteredReport,
            String status,
            String actualResult
    ) {}
}
