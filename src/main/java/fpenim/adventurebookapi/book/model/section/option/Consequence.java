package fpenim.adventurebookapi.book.model.section.option;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Consequence(
        @NotNull ConsequenceType type,
        @Positive int value,
        String text
) {}
