package fpenim.adventurebookapi.book.dto;

import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookImportRequest(
        @NotBlank String title,
        @NotBlank String author,
        List<@NotBlank String> categories,
        @NotNull Difficulty difficulty,
        @NotEmpty List<@NotNull @Valid Section> sections
) {
    public BookImportRequest {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}