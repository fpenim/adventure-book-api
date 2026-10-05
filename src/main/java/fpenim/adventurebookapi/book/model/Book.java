package fpenim.adventurebookapi.book.model;

import fpenim.adventurebookapi.book.model.section.Section;

import java.util.List;

public record Book(
        int id,
        String title,
        String author,
        List<String> categories,
        Difficulty difficulty,
        List<Section> sections
) {}
