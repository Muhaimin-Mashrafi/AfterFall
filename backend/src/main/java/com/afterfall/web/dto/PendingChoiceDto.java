package com.afterfall.web.dto;

import java.util.List;

public record PendingChoiceDto(
        String id,
        String title,
        String prompt,
        List<ChoiceOptionDto> options
) {}
