package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.RunCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataRunCheckpointRepository extends JpaRepository<RunCheckpointEntity, Long> {
    List<RunCheckpointEntity> findByRunIdOrderByTickAsc(String runId);
}
