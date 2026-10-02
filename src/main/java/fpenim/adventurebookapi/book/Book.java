package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.section.Section;

import java.util.List;

public record Book(
        String title,
        String author,
        List<String> categories,
        Difficulty difficulty,
        List<Section> sections
) {}
