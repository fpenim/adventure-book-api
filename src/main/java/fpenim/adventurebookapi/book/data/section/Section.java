package fpenim.adventurebookapi.book.data.section;

import fpenim.adventurebookapi.book.data.section.option.Option;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record Section(
        @NotNull int id,
        @NotBlank String text,
        @NotNull SectionType sectionType,
        List<@NotNull @Valid Option> options
) {
    public Section {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
