package com.afterfall.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;

class ActionTest {

    private Survivor createSampleSurvivor(int id) {
        return new Survivor(
                new SurvivorId(id),
                "Survivor " + id,
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 60),
                Needs.fullySatisfied(),
                Health.healthy(),
                new Inventory(10)
        );
    }

    @Test
    void eatActionRequiresFoodAndProposesCorrectChanges() {
        EatAction action = new EatAction();
        assertEquals(ActionType.EAT, action.type());

        Survivor survivor = createSampleSurvivor(1);
        survivor.needs().set(Need.FOOD, 40);

        // Case 1: No food anywhere -> rejected
        ActionContext contextNoFood = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        ActionResult rejectedResult = action.evaluate(contextNoFood);
        assertFalse(rejectedResult.isSuccess());
        assertEquals(ActionStatus.REJECTED, rejectedResult.status());
        assertTrue(rejectedResult.needDeltas().isEmpty());
        assertTrue(rejectedResult.inventoryDeltas().isEmpty());

        // Case 2: Personal food available
        survivor.inventory().add(ItemType.FOOD, 2);
        ActionContext contextWithFood = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        ActionResult successResult = action.evaluate(contextWithFood);
        assertTrue(successResult.isSuccess());
        assertEquals(EatAction.HUNGER_REDUCTION, successResult.needDeltas().get(Need.FOOD));
        assertEquals(-1, successResult.inventoryDeltas().get(ItemType.FOOD));
        assertEquals(1, successResult.eventProposals().size());

        // Domain survivor state was untouched during evaluation
        assertEquals(40, survivor.needs().level(Need.FOOD));
        assertEquals(2, survivor.inventory().quantity(ItemType.FOOD));
    }

    @Test
    void workActionRespectsSkillAndFatigue() {
        WorkAction action = new WorkAction();
        assertEquals(ActionType.WORK, action.type());

        Survivor survivor = createSampleSurvivor(2);
        survivor.needs().set(Need.REST, 80);

        ActionContext context = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        ActionResult result = action.evaluate(context);
        assertTrue(result.isSuccess());
        assertEquals(WorkAction.BASE_REST_COST, result.needDeltas().get(Need.REST));
        // Skill 60 -> 6 work contribution
        assertEquals(6, result.workContribution());
        assertEquals(1, result.eventProposals().size());

        // Exhausted survivor cannot work
        survivor.needs().set(Need.REST, 10);
        ActionContext contextTired = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        ActionResult rejected = action.evaluate(contextTired);
        assertFalse(rejected.isSuccess());
        assertEquals(0, rejected.workContribution());
    }

    @Test
    void restActionProposesEnergyRestoration() {
        RestAction action = new RestAction();
        assertEquals(ActionType.REST, action.type());

        Survivor survivor = createSampleSurvivor(3);
        survivor.needs().set(Need.REST, 30);

        ActionContext context = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        ActionResult result = action.evaluate(context);
        assertTrue(result.isSuccess());
        assertEquals(RestAction.REST_RESTORE, result.needDeltas().get(Need.REST));
        assertEquals(1, result.eventProposals().size());
        assertEquals(30, survivor.needs().level(Need.REST));
    }

    @Test
    void healActionRequiresIllnessAndMedicine() {
        HealAction action = new HealAction();
        assertEquals(ActionType.HEAL, action.type());

        Survivor survivor = createSampleSurvivor(4);
        // Already healthy -> rejected
        ActionContext contextHealthy = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );
        ActionResult resHealthy = action.evaluate(contextHealthy);
        assertFalse(resHealthy.isSuccess());

        // Ill, but no medicine -> rejected
        survivor.health().setCondition(HealthCondition.ILL);
        survivor.health().setVitality(40);
        ActionContext contextNoMed = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                false
        );
        ActionResult resNoMed = action.evaluate(contextNoMed);
        assertFalse(resNoMed.isSuccess());

        // Ill, with personal medicine -> success
        survivor.inventory().add(ItemType.MEDICINE, 1);
        ActionContext contextMed = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                false
        );
        ActionResult resSuccess = action.evaluate(contextMed);
        assertTrue(resSuccess.isSuccess());
        assertEquals(HealAction.VITALITY_RECOVERY, resSuccess.vitalityDelta());
        assertEquals(HealthCondition.HEALTHY, resSuccess.newCondition().orElseThrow());
        assertEquals(-1, resSuccess.inventoryDeltas().get(ItemType.MEDICINE));

        // Original survivor remained unmodified
        assertEquals(HealthCondition.ILL, survivor.health().condition());
        assertEquals(40, survivor.health().vitality());
        assertEquals(1, survivor.inventory().quantity(ItemType.MEDICINE));
    }

    @Test
    void moveActionValidatesDestinationWithoutMutatingDomainState() {
        MoveAction action = new MoveAction(RegionId.REGION_2);
        assertEquals(ActionType.MOVE, action.type());
        assertEquals(RegionId.REGION_2, action.destination());

        Survivor survivor = createSampleSurvivor(5);

        // Case 1: Destination is identical to current -> rejected
        ActionContext sameRegionContext = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_2,
                false,
                false
        );
        ActionResult rejectedSame = action.evaluate(sameRegionContext);
        assertFalse(rejectedSame.isSuccess());

        // Case 2: Destination is different -> success
        ActionContext diffRegionContext = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                false,
                false
        );
        ActionResult successMove = action.evaluate(diffRegionContext);
        assertTrue(successMove.isSuccess());
        assertEquals(RegionId.REGION_2, successMove.migrationRequest().orElseThrow());

        // Survivor location is completely unmodified
        assertTrue(survivor.location().isEmpty());
    }

    @Test
    void deadSurvivorIsRejectedByAllActions() {
        Survivor survivor = createSampleSurvivor(6);
        survivor.markDead();
        ActionContext context = new ActionContext(
                SurvivorSnapshot.from(survivor),
                RegionId.REGION_1,
                true,
                true
        );

        assertFalse(new EatAction().evaluate(context).isSuccess());
        assertFalse(new WorkAction().evaluate(context).isSuccess());
        assertFalse(new RestAction().evaluate(context).isSuccess());
        assertFalse(new HealAction().evaluate(context).isSuccess());
        assertFalse(new MoveAction(RegionId.REGION_3).evaluate(context).isSuccess());
    }

    @Test
    void actionResultCollectionsAreImmutable() {
        ActionResult result = ActionResult.builder(ActionType.WORK)
                .needDelta(Need.REST, -10)
                .inventoryDelta(ItemType.FOOD, 1)
                .addEvent(new ProposedEvent("TEST", new SurvivorId(1), "test", 1))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> result.needDeltas().put(Need.SAFETY, 5));
        assertThrows(UnsupportedOperationException.class,
                () -> result.inventoryDeltas().put(ItemType.WATER, 2));
        assertThrows(UnsupportedOperationException.class,
                () -> result.eventProposals().clear());
    }
}
