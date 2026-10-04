package fpenim.adventurebookapi.book.data;

import fpenim.adventurebookapi.book.data.section.Section;

import java.util.List;

public record Book(
        int id,
        String title,
        String author,
        List<String> categories,
        Difficulty difficulty,
        List<Section> sections
) {}
