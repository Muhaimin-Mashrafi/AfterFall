package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.SimulationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSimulationEventRepository extends JpaRepository<SimulationEventEntity, SimulationEventEntity.EventCompositeKey> {
    List<SimulationEventEntity> findByRunIdOrderByEventIdAsc(String runId);
}
