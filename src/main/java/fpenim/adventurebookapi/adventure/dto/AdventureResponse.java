package fpenim.adventurebookapi.adventure.dto;

import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import fpenim.adventurebookapi.book.dto.SectionResponse;

public record AdventureResponse(
        String username, Long bookId, int health, AdventureStatus status, SectionResponse section) {}
