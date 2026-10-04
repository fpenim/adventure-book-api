package fpenim.adventurebookapi.book.dto;

import fpenim.adventurebookapi.book.data.Difficulty;

import java.util.List;

public record BookSummaryResponse(
        Long id,
        String title,
        String author,
        List<String> categories,
        Difficulty difficulty
) {}
