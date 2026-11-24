package com.afterfall.web.dto;

import java.util.List;

public record SurvivorDto(
        int id,
        int region,
        boolean isAlive,
        String profession,
        String personality,
        MapDto needs,
        int trustInObserver,
        List<String> relationships,
        List<String> memories
) {
    public record MapDto(int hunger, int thirst, int fatigue, int health) {}
}
