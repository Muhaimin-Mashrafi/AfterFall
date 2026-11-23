package com.afterfall.narrator;

import java.util.Objects;

/**
 * An authored narrative template pattern.
 *
 * <p>Uses placeholders like {survivor}, {target}, {region}, {day}, {resource}, {amount}.
 */
public record NarrativeTemplate(
        String id,
        String eventType,
        String pattern
) {
    public NarrativeTemplate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(pattern, "pattern");
    }

    public String render(String survivorName, String targetName, String regionName, long day, String extra) {
        String res = pattern;
        if (survivorName != null) res = res.replace("{survivor}", survivorName);
        if (targetName != null) res = res.replace("{target}", targetName);
        if (regionName != null) res = res.replace("{region}", regionName);
        res = res.replace("{day}", String.valueOf(day));
        if (extra != null) res = res.replace("{extra}", extra);
        return res;
    }
}
