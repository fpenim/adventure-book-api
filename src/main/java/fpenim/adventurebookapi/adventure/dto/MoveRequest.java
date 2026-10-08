package fpenim.adventurebookapi.adventure.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MoveRequest(@NotNull @PositiveOrZero Integer option) {}
