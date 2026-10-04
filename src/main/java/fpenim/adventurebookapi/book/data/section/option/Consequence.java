package fpenim.adventurebookapi.book.data.section.option;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Consequence(
        @NotNull ConsequenceType type,
        @NotNull @Positive int value,
        String text
) {}
