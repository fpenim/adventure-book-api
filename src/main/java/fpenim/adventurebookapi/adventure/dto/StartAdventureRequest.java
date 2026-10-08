package fpenim.adventurebookapi.adventure.dto;

import jakarta.validation.constraints.NotNull;

public record StartAdventureRequest(@NotNull Long bookId) {}
