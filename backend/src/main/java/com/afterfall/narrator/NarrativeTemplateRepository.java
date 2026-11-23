package com.afterfall.narrator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository of at least 40 authored narrative template variants across all event types.
 *
 * <p>Maintains the quiet, restrained, non-graphic cinematic register requested by Section 6.4.
 * Banned graphic words are strictly excluded.
 */
public final class NarrativeTemplateRepository {

    /**
     * Forbidden terms as mandated by blueprint Section 6.4 to prevent sensationalist or graphic depictions.
     */
    public static final Set<String> BANNED_TERMS = Set.of(
            "suicide", "corpse", "slaughter", "bloodbath", "hanging", "gore", "murder", "mutilate", "kill yourself"
    );

    private static final List<NarrativeTemplate> TEMPLATES = List.of(
            // --- EAT / FOOD (1-5) ---
            new NarrativeTemplate("EAT_1", "SURVIVOR_EAT", "Day {day}. {survivor} consumed a measured meal in quiet isolation."),
            new NarrativeTemplate("EAT_2", "SURVIVOR_EAT", "Day {day}. In {region}, {survivor} divided their daily ration with careful hands."),
            new NarrativeTemplate("EAT_3", "SURVIVOR_EAT", "{survivor} broke bread alone as dusk settled over {region}."),
            new NarrativeTemplate("EAT_4", "SURVIVOR_EAT", "A brief pause in the work: {survivor} shared silent sustenance."),
            new NarrativeTemplate("EAT_5", "SURVIVOR_EAT", "Day {day}. {survivor} finished the last preserved rations without ceremony."),

            // --- WORK / LABOR (6-11) ---
            new NarrativeTemplate("WORK_1", "SURVIVOR_WORK", "Day {day}. {survivor} laboured through the cold morning to reinforce the perimeter."),
            new NarrativeTemplate("WORK_2", "SURVIVOR_WORK", "Dust gathered on hands and tools as {survivor} finished their assigned shift in {region}."),
            new NarrativeTemplate("WORK_3", "SURVIVOR_WORK", "{survivor} tended the communal facilities under the low grey sky."),
            new NarrativeTemplate("WORK_4", "SURVIVOR_WORK", "Day {day}. The rhythm of repairs in {region} continued under {survivor}'s quiet effort."),
            new NarrativeTemplate("WORK_5", "SURVIVOR_WORK", "Without complaint, {survivor} secured the supply cache for another day."),
            new NarrativeTemplate("WORK_6", "SURVIVOR_WORK", "Another shift completed: {survivor} counted what little the earth had yielded."),

            // --- REST / RECOVERY (12-16) ---
            new NarrativeTemplate("REST_1", "SURVIVOR_REST", "Day {day}. Exhaustion settled in; {survivor} rested quietly in the barracks."),
            new NarrativeTemplate("REST_2", "SURVIVOR_REST", "{survivor} found shelter from the wind and slept until dusk."),
            new NarrativeTemplate("REST_3", "SURVIVOR_REST", "Night arrived in {region}. {survivor} lay motionless, listening to the silence."),
            new NarrativeTemplate("REST_4", "SURVIVOR_REST", "Day {day}. Weariness took its toll as {survivor} withdrew to recover strength."),
            new NarrativeTemplate("REST_5", "SURVIVOR_REST", "A rare hour of quiet stillness as {survivor} kept vigil before resting."),

            // --- HEAL / MEDICAL (17-21) ---
            new NarrativeTemplate("HEAL_1", "SURVIVOR_HEAL", "Day {day}. {survivor} received treatment, colour slowly returning to pale skin."),
            new NarrativeTemplate("HEAL_2", "SURVIVOR_HEAL", "The medical remedies in {region} took hold; {survivor}'s fever broke before dawn."),
            new NarrativeTemplate("HEAL_3", "SURVIVOR_HEAL", "{survivor} applied bandages with steady fingers, recovering vital breath."),
            new NarrativeTemplate("HEAL_4", "SURVIVOR_HEAL", "Day {day}. A measure of medicine gave {survivor} another chance to walk."),
            new NarrativeTemplate("HEAL_5", "SURVIVOR_HEAL", "Through scarce clinical stores, {survivor} was pulled back from the brink."),

            // --- MOVE / MIGRATION (22-26) ---
            new NarrativeTemplate("MOVE_1", "SURVIVOR_MOVE", "Day {day}. {survivor} gathered their few belongings and departed for {region}."),
            new NarrativeTemplate("MOVE_2", "SURVIVOR_MOVE", "Footsteps in the gravel marked {survivor}'s relocation toward new quarters."),
            new NarrativeTemplate("MOVE_3", "SURVIVOR_MOVE", "{survivor} left behind the quiet quadrant to seek shelter in {region}."),
            new NarrativeTemplate("MOVE_4", "SURVIVOR_MOVE", "Day {day}. A solitary figure moved between zones under the watching towers."),
            new NarrativeTemplate("MOVE_5", "SURVIVOR_MOVE", "Crossing the boundary markers, {survivor} entered {region} without looking back."),

            // --- CRISIS: FAMINE (27-31) ---
            new NarrativeTemplate("FAMINE_1", "CRISIS_FAMINE", "Day {day}. The storehouses were nearly bare; a hollow silence gripped the settlement."),
            new NarrativeTemplate("FAMINE_2", "CRISIS_FAMINE", "Famine took root. Survivors moved with slow, deliberate steps to conserve vitality."),
            new NarrativeTemplate("FAMINE_3", "CRISIS_FAMINE", "Day {day}. Rations ran dry; empty tables cast long shadows in {region}."),
            new NarrativeTemplate("FAMINE_4", "CRISIS_FAMINE", "Scarcity deepened across all quadrants as hunger gnawed at communal patience."),
            new NarrativeTemplate("FAMINE_5", "CRISIS_FAMINE", "Day {day}. Despair gathered quietly around the dried grain bins."),

            // --- CRISIS: DISEASE OUTBREAK (32-36) ---
            new NarrativeTemplate("DISEASE_1", "CRISIS_DISEASE", "Day {day}. Coughing echoed across the quarters; contagion spread swiftly."),
            new NarrativeTemplate("DISEASE_2", "CRISIS_DISEASE", "Illness deepened its hold. Quarantined doors remained closed throughout {region}."),
            new NarrativeTemplate("DISEASE_3", "CRISIS_DISEASE", "Day {day}. A persistent fever claimed the strength of another household."),
            new NarrativeTemplate("DISEASE_4", "CRISIS_DISEASE", "The infirmary in {region} overflowed as medical supplies dwindled."),
            new NarrativeTemplate("DISEASE_5", "CRISIS_DISEASE", "Anxious glances passed between neighbours as sickness tightened its grip."),

            // --- OBSERVER INTERVENTIONS (37-42) ---
            new NarrativeTemplate("OBS_1", "OBSERVER_DISTORTION", "Day {day}. Unsettling rumours of hidden caches spread through {region}."),
            new NarrativeTemplate("OBS_2", "OBSERVER_TRUST_EROSION", "Words whispered in the dark widened the rift between {survivor} and {target}."),
            new NarrativeTemplate("OBS_3", "OBSERVER_RESOURCE_PRESSURE", "Day {day}. Official notices of impending scarcity sparked guarded competition."),
            new NarrativeTemplate("OBS_4", "OBSERVER_SELECTIVE_DISCLOSURE", "Selective advisories regarding infection stoked suspicion toward {survivor}."),
            new NarrativeTemplate("OBS_5", "OBSERVER_DISTORTION", "Discrepancies in ration logs left survivors questioning what was being kept from them."),
            new NarrativeTemplate("OBS_6", "OBSERVER_TRUST_EROSION", "Trust eroded silently; old alliances dissolved into watchful caution."),

            // --- LOSS / SILENCE (IMPLIED NON-GRAPHIC) (43-46) ---
            new NarrativeTemplate("LOSS_1", "SURVIVOR_DEATH", "Day {day}. The east shelter stood vacant by morning; {survivor}'s journey had ended."),
            new NarrativeTemplate("LOSS_2", "SURVIVOR_DEATH", "A quiet mantle of grief settled as {survivor} did not answer the morning muster."),
            new NarrativeTemplate("LOSS_3", "SURVIVOR_DEATH", "Her chair stayed turned toward the window; {survivor} was spoken of only in whispers."),
            new NarrativeTemplate("LOSS_4", "SURVIVOR_DEATH", "Day {day}. Another voice fell silent in the bitter chill of {region}.")
    );

    public static List<NarrativeTemplate> allTemplates() {
        return Collections.unmodifiableList(TEMPLATES);
    }

    public static List<NarrativeTemplate> templatesFor(String eventType) {
        return TEMPLATES.stream()
                .filter(t -> t.eventType().equalsIgnoreCase(eventType))
                .toList();
    }
}
