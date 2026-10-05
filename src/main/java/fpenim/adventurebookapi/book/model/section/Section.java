package fpenim.adventurebookapi.book.model.section;

import fpenim.adventurebookapi.book.model.section.option.Option;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record Section(
        @Positive int id, @NotBlank String text, @NotNull SectionType type, List<@NotNull @Valid Option> options) {
    public Section {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
