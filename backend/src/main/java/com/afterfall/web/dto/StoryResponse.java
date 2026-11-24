package com.afterfall.web.dto;

import java.util.List;

public record StoryResponse(
        String currentChapter,
        long tick,
        List<CaptionDto> recentCaptions,
        ChoiceMomentDto activeChoice
) {
    public record CaptionDto(long captionId, long tick, String text, String severity, List<Long> sourceEvents) {}
    public record ChoiceMomentDto(String choiceId, String dilemmaText, String chapter, List<OptionDto> options) {}
    public record OptionDto(String optionId, String label, String consequencePreview) {}
}
