package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.data.Difficulty;

public record BookFilter(
        String title,
        String author,
        String category,
        Difficulty difficulty
) {}
