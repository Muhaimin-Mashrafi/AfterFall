package com.afterfall.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.afterfall.action.Action;
import com.afterfall.action.ActionType;
import com.afterfall.domain.Health;
import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.Inventory;
import com.afterfall.domain.ItemType;
import com.afterfall.domain.Need;
import com.afterfall.domain.Needs;
import com.afterfall.domain.Personality;
import com.afterfall.domain.Profession;
import com.afterfall.domain.ProfessionType;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.SurvivorSnapshot;
import com.afterfall.domain.Trait;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BehaviorTest {

    private Survivor createSampleSurvivor(int id) {
        return new Survivor(
                new SurvivorId(id),
                "Survivor " + id,
                Personality.neutral(),
                new Profession(ProfessionType.MEDIC, 70),
                Needs.fullySatisfied(),
                Health.healthy(),
                new Inventory(10)
        );
    }

    @Test
    void seekFoodBehaviorTriggersWhenHungryAndFoodAvailable() {
        SeekFoodBehavior behavior = new SeekFoodBehavior();
        assertEquals(BehaviorType.SEEK_FOOD, behavior.type());

        Survivor survivor = createSampleSurvivor(1);
        survivor.needs().set(Need.FOOD, 40); // Below hunger threshold 50

        // Food not available -> cannot decide to eat
        BehaviorContext noFoodCtx = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        assertTrue(behavior.decide(noFoodCtx).isEmpty());

        // Food available -> selects EatAction
        BehaviorContext foodCtx = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                false
        );
        Optional<Action> action = behavior.decide(foodCtx);
        assertTrue(action.isPresent());
        assertEquals(ActionType.EAT, action.get().type());
    }

    @Test
    void restBehaviorTriggersWhenFatigued() {
        RestBehavior behavior = new RestBehavior();
        assertEquals(BehaviorType.REST, behavior.type());

        Survivor survivor = createSampleSurvivor(2);
        survivor.needs().set(Need.REST, 40); // Below default threshold 45

        BehaviorContext context = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        Optional<Action> action = behavior.decide(context);
        assertTrue(action.isPresent());
        assertEquals(ActionType.REST, action.get().type());

        // Fully rested survivor -> does not rest
        survivor.needs().set(Need.REST, 90);
        BehaviorContext restedContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        assertTrue(behavior.decide(restedContext).isEmpty());
    }

    @Test
    void seekMedicalHelpBehaviorSelectsHealOrMove() {
        SeekMedicalHelpBehavior behavior = new SeekMedicalHelpBehavior();
        assertEquals(BehaviorType.SEEK_MEDICAL_HELP, behavior.type());

        Survivor survivor = createSampleSurvivor(3);
        survivor.health().setCondition(HealthCondition.INJURED);

        // Case 1: Medicine available locally -> selects HealAction
        BehaviorContext healContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                true
        );
        Optional<Action> healAction = behavior.decide(healContext);
        assertTrue(healAction.isPresent());
        assertEquals(ActionType.HEAL, healAction.get().type());

        // Case 2: No local medicine, but candidate regions available -> selects MoveAction
        BehaviorContext moveContext = BehaviorContext.withCandidates(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false,
                List.of(RegionId.REGION_2, RegionId.REGION_3)
        );
        Optional<Action> moveAction = behavior.decide(moveContext);
        assertTrue(moveAction.isPresent());
        assertEquals(ActionType.MOVE, moveAction.get().type());
    }

    @Test
    void workBehaviorRespectsHealthAndProfessions() {
        WorkBehavior behavior = new WorkBehavior();
        assertEquals(BehaviorType.WORK, behavior.type());

        Survivor survivor = createSampleSurvivor(4);
        BehaviorContext healthyContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        Optional<Action> action = behavior.decide(healthyContext);
        assertTrue(action.isPresent());
        assertEquals(ActionType.WORK, action.get().type());

        // Injured survivor refuses to work
        survivor.health().setCondition(HealthCondition.INJURED);
        BehaviorContext injuredContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        assertTrue(behavior.decide(injuredContext).isEmpty());
    }

    @Test
    void behaviorSelectorPrioritizesCriticalNeedsDeterministically() {
        BehaviorSelector selector = new BehaviorSelector();

        Survivor survivor = createSampleSurvivor(5);

        // Scenario 1: Critically injured -> Medical help prioritized over hunger & work
        survivor.health().setCondition(HealthCondition.ILL);
        survivor.needs().set(Need.FOOD, 20); // Critical hunger
        survivor.needs().set(Need.REST, 15); // Critical exhaustion
        survivor.inventory().add(ItemType.MEDICINE, 1);
        survivor.inventory().add(ItemType.FOOD, 1);

        BehaviorContext criticalContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        Action chosen1 = selector.selectAction(criticalContext);
        assertEquals(ActionType.HEAL, chosen1.type());

        // Scenario 2: Healthy, but critical starvation -> Eat prioritized over work
        survivor.health().setCondition(HealthCondition.HEALTHY);
        BehaviorContext healthyHungryContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        Action chosen2 = selector.selectAction(healthyHungryContext);
        assertEquals(ActionType.EAT, chosen2.type());

        // Scenario 3: Healthy, fed, exhausted -> Rest prioritized
        survivor.needs().set(Need.FOOD, 80);
        survivor.needs().set(Need.REST, 15);
        BehaviorContext tiredContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        Action chosen3 = selector.selectAction(tiredContext);
        assertEquals(ActionType.REST, chosen3.type());

        // Scenario 4: All needs satisfied -> Work selected
        survivor.needs().set(Need.REST, 80);
        BehaviorContext readyContext = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        Action chosen4 = selector.selectAction(readyContext);
        assertEquals(ActionType.WORK, chosen4.type());
    }

    @Test
    void deceasedSurvivorThrowsOnBehaviorSelection() {
        BehaviorSelector selector = new BehaviorSelector();
        Survivor survivor = createSampleSurvivor(6);
        survivor.markDead();

        BehaviorContext context = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        assertThrows(IllegalArgumentException.class, () -> selector.selectAction(context));
    }

    @Test
    void personalityInfluencesThresholdsWithoutOverridingCriticalNeeds() {
        SeekFoodBehavior behavior = new SeekFoodBehavior();
        Survivor fearfulSurvivor = createSampleSurvivor(7);
        // Trait FEARFUL > 60 increases hunger threshold
        Personality fearfulPersonality = Personality.neutral().with(Trait.FEARFUL, 85);
        Survivor survivor = new Survivor(
                new SurvivorId(7),
                "Fearful",
                fearfulPersonality,
                new Profession(ProfessionType.GUARD, 50),
                new Needs(55, 100, 100, 100), // Food 55: above default 50, but <= 55 with bonus
                Health.healthy(),
                new Inventory(5)
        );
        survivor.inventory().add(ItemType.FOOD, 1);

        BehaviorContext context = BehaviorContext.of(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                false
        );
        Optional<Action> action = behavior.decide(context);
        assertTrue(action.isPresent(), "Fearful personality should seek food at slightly higher threshold");
        assertEquals(ActionType.EAT, action.get().type());
    }
}
