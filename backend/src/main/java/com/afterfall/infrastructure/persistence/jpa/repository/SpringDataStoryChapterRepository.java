package com.afterfall.infrastructure.persistence.jpa.repository;

import com.afterfall.infrastructure.persistence.jpa.entity.StoryChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataStoryChapterRepository extends JpaRepository<StoryChapterEntity, Long> {
    List<StoryChapterEntity> findByRunIdOrderByTickAsc(String runId);
}
