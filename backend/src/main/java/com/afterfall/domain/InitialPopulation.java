package com.afterfall.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic generator of the initial settlement population.
 *
 * <p>Supports every valid population count from {@value PopulationConfig#MIN_POPULATION} to
 * {@value PopulationConfig#MAX_POPULATION} inclusive. Assigns survivors deterministically
 * across the four processing regions in round-robin sequence to ensure balanced regional distribution.
 */
public final class InitialPopulation {

    private static final String[] NAMES = {
            "Mara Vance", "Jonas Reed", "Elena Rostova", "Kaelen Voss", "Sarah Chen",
            "Tarek Morales", "David Lind", "Nadia Mercer", "Marcus Thorne", "Clara Bell",
            "Aaron Cross", "Maya Lin", "Lucas Drake", "Sofia Ruiz", "Gabriel Holt",
            "Lila Hayes", "Nathan Ward", "Zoe Bennett", "Owen Fletcher", "Tessa Brooks",
            "Julian Cole", "Ananya Roy", "Kira Sterling", "Devin Mercer", "Iris Becker",
            "Leon Castillo", "Vera Novak", "Rowan Finch", "Hanna Frost", "Caleb Stone"
    };

    private static final ProfessionType[] PROFESSION_ROTATION = {
            ProfessionType.FARMER,
            ProfessionType.SCAVENGER,
            ProfessionType.MEDIC,
            ProfessionType.GUARD,
            ProfessionType.GENERAL_WORKER
    };

    private InitialPopulation() {
    }

    /** Creates a settlement using the default population configuration (25 survivors). */
    public static Settlement createDefaultSettlement(String settlementName) {
        return createSettlement(settlementName, PopulationConfig.defaultConfig());
    }

    /** Creates a settlement populated according to the given PopulationConfig. */
    public static Settlement createSettlement(String settlementName, PopulationConfig config) {
        Objects.requireNonNull(config, "config");
        Settlement settlement = new Settlement(settlementName);
        List<Survivor> initialSurvivors = generateSurvivors(config.size());

        List<RegionId> regionIds = RegionId.ALL;
        for (int i = 0; i < initialSurvivors.size(); i++) {
            Survivor survivor = initialSurvivors.get(i);
            RegionId assignedRegion = regionIds.get(i % regionIds.size());
            settlement.registerSurvivor(survivor, assignedRegion);
        }

        return settlement;
    }

    /** Creates a world initialized with a settlement of the default population size. */
    public static World createDefaultWorld(String settlementName) {
        return new World(createDefaultSettlement(settlementName));
    }

    /** Creates a world initialized with the given population configuration. */
    public static World createWorld(String settlementName, PopulationConfig config) {
        return new World(createSettlement(settlementName, config));
    }

    /** Generates a list of deterministic survivors with varied traits and MUST professions. */
    public static List<Survivor> generateSurvivors(int count) {
        PopulationConfig config = new PopulationConfig(count); // Validates count in [20, 30]

        return java.util.stream.IntStream.range(0, config.size())
                .mapToObj(i -> {
                    SurvivorId id = new SurvivorId(i + 1);
                    String name = NAMES[i];
                    ProfessionType profType = PROFESSION_ROTATION[i % PROFESSION_ROTATION.length];
                    int skill = 40 + ((i * 7) % 45); // Deterministic skill in [40, 84]
                    Profession profession = new Profession(profType, skill);

                    Personality personality = createPersonality(i);
                    Needs needs = new Needs(
                            70 + ((i * 3) % 25), // food
                            70 + ((i * 5) % 25), // water
                            65 + ((i * 4) % 30), // rest
                            60 + ((i * 6) % 35)  // safety
                    );
                    Health health = new Health(80 + ((i * 2) % 21), HealthCondition.HEALTHY);
                    Inventory inventory = new Inventory(10);
                    // Minimal starter inventory: 1 food, 1 water
                    inventory.add(ItemType.FOOD, 1);
                    inventory.add(ItemType.WATER, 1);

                    return new Survivor(id, name, personality, profession, needs, health, inventory);
                })
                .toList();
    }

    private static Personality createPersonality(int index) {
        Map<Trait, Integer> traits = new EnumMap<>(Trait.class);
        traits.put(Trait.COMPASSIONATE, 30 + ((index * 11) % 60));
        traits.put(Trait.SELF_INTERESTED, 25 + ((index * 13) % 65));
        traits.put(Trait.FEARFUL, 20 + ((index * 17) % 60));
        traits.put(Trait.AGGRESSIVE, 20 + ((index * 7) % 55));
        traits.put(Trait.COOPERATIVE, 35 + ((index * 9) % 60));
        traits.put(Trait.SUSPICIOUS, 25 + ((index * 19) % 65));
        traits.put(Trait.RESILIENT, 40 + ((index * 5) % 55));
        traits.put(Trait.AUTHORITATIVE, 20 + ((index * 23) % 60));
        return new Personality(traits);
    }
}
