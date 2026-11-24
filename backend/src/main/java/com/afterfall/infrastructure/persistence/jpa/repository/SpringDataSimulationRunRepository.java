package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.SimulationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSimulationRunRepository extends JpaRepository<SimulationRunEntity, String> {
}
