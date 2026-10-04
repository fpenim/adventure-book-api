package fpenim.adventurebookapi.book.data.section.option;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record Option(
        @NotBlank String description,
        @NotNull int gotoId,
        List<@Valid Consequence> consequences
) {}
