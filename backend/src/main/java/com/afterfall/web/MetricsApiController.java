package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.web.dto.MetricsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MetricsApiController {

    private final SimulationService simulationService;

    public MetricsApiController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponse> getMetrics() {
        return ResponseEntity.ok(simulationService.getMetrics());
    }
}
