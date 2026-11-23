package com.afterfall.narrator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeTemplateRepositoryTest {

    @Test
    void atLeastFortyAuthoredTemplatesExist() {
        List<NarrativeTemplate> templates = NarrativeTemplateRepository.allTemplates();
        assertThat(templates.size()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void uniqueTemplateIds() {
        List<NarrativeTemplate> templates = NarrativeTemplateRepository.allTemplates();
        long uniqueCount = templates.stream().map(NarrativeTemplate::id).distinct().count();
        assertThat(uniqueCount).isEqualTo(templates.size());
    }

    /**
     * Blueprint Section 6.4: Banned-term keyword test over all templates.
     * Fails build if any graphic or sensationalist word appears in any template.
     */
    @Test
    void noBannedTermsInAnyTemplate() {
        Set<String> banned = NarrativeTemplateRepository.BANNED_TERMS;
        for (NarrativeTemplate template : NarrativeTemplateRepository.allTemplates()) {
            String lowerPattern = template.pattern().toLowerCase();
            for (String forbidden : banned) {
                assertThat(lowerPattern)
                        .withFailMessage("Template " + template.id() + " contains banned term: '" + forbidden + "'")
                        .doesNotContain(forbidden.toLowerCase());
            }
        }
    }

    @Test
    void placeholderRenderingIsAccurate() {
        NarrativeTemplate t = new NarrativeTemplate("TEST", "SURVIVOR_WORK", "Day {day}. {survivor} laboured in {region}.");
        String rendered = t.render("Alice", null, "Quadrant 1", 3, null);
        assertThat(rendered).isEqualTo("Day 3. Alice laboured in Quadrant 1.");
    }
}
