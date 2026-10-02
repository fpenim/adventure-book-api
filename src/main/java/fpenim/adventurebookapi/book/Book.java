package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.section.Section;

import java.util.List;

public record Book(
        String title,
        String author,
        Difficulty difficulty,
        List<Section> sections
) {}
