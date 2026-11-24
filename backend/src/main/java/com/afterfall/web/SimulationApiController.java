package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.web.dto.StateResponse;
import com.afterfall.web.dto.SurvivorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SimulationApiController {

    private final SimulationService simulationService;

    public SimulationApiController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/state")
    public ResponseEntity<StateResponse> getState() {
        return ResponseEntity.ok(simulationService.getCurrentState());
    }

    @PostMapping("/sim/start")
    public ResponseEntity<StateResponse> start() {
        return ResponseEntity.ok(simulationService.startSimulation());
    }

    @PostMapping("/sim/pause")
    public ResponseEntity<StateResponse> pause() {
        return ResponseEntity.ok(simulationService.pauseSimulation());
    }

    @PostMapping("/sim/resume")
    public ResponseEntity<StateResponse> resume() {
        return ResponseEntity.ok(simulationService.resumeSimulation());
    }

    @PostMapping("/sim/step")
    public ResponseEntity<StateResponse> step() {
        return ResponseEntity.ok(simulationService.stepTick());
    }

    @GetMapping("/survivors")
    public ResponseEntity<List<SurvivorDto>> getSurvivors() {
        return ResponseEntity.ok(simulationService.getAllSurvivors());
    }

    @GetMapping("/survivors/{id}")
    public ResponseEntity<SurvivorDto> getSurvivor(@PathVariable String id) {
        return simulationService.getSurvivor(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
