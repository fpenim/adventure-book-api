package fpenim.adventurebookapi.adventure.dto;

import fpenim.adventurebookapi.adventure.model.AdventureStatus;

public record AdventureResponse(
        Long id, String username, Long bookId, int health, AdventureStatus status, AdventureSectionResponse section) {}
