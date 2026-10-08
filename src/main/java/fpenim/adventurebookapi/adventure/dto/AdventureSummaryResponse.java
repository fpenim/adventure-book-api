package fpenim.adventurebookapi.adventure.dto;

import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "adventures")
public record AdventureSummaryResponse(
        Long id, Long bookId, String bookTitle, int currentSectionId, int health, AdventureStatus status) {}
