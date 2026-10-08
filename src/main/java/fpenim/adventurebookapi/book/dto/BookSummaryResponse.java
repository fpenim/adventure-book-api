package fpenim.adventurebookapi.book.dto;

import fpenim.adventurebookapi.book.model.Difficulty;
import java.util.List;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "books")
public record BookSummaryResponse(
        Long id, String title, String author, List<String> categories, Difficulty difficulty, int beginSectionId) {}
