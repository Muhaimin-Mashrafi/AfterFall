package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.web.dto.ChoiceSelectionRequest;
import com.afterfall.web.dto.StateResponse;
import com.afterfall.web.dto.StoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StoryApiController {

    private final SimulationService simulationService;

    public StoryApiController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/story")
    public ResponseEntity<StoryResponse> getStory() {
        return ResponseEntity.ok(simulationService.getStory());
    }

    @PostMapping("/choices/{choiceId}")
    public ResponseEntity<StateResponse> submitChoice(
            @PathVariable String choiceId,
            @RequestBody ChoiceSelectionRequest request) {

        int optIdx = request.optionIndex() != null ? request.optionIndex() : 0;
        StateResponse updated = simulationService.choose(choiceId, optIdx);
        return ResponseEntity.ok(updated);
    }
}
