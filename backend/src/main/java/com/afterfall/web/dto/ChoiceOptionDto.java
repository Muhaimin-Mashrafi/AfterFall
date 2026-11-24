package com.afterfall.web.dto;

public record ChoiceOptionDto(
        String optionId,
        String label,
        String description,
        String consequencePreview
) {}
