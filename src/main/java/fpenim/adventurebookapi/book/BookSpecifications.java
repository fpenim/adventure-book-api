package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.repository.BookEntity;
import java.util.Locale;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {}

    public static Specification<BookEntity> matches(BookFilter filter) {
        return titleContains(filter.title())
                .and(authorContains(filter.author()))
                .and(hasCategory(filter.category()))
                .and(hasDifficulty(filter.difficulty()));
    }

    public static Specification<BookEntity> titleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("title")), containsPattern(title), '!');
        };
    }

    public static Specification<BookEntity> authorContains(String author) {
        return (root, query, cb) -> {
            if (author == null || author.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("author")), containsPattern(author), '!');
        };
    }

    public static Specification<BookEntity> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> {
            if (difficulty == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("difficulty"), difficulty);
        };
    }

    public static Specification<BookEntity> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) {
                return cb.conjunction();
            }

            String normalized = category.strip().toLowerCase();

            HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;

            return hcb.isTrue(hcb.sql("""
                            exists (
                                select 1
                                from unnest(?) as category_item(value)
                                where lower(category_item.value) = ?
                            )
                            """, Boolean.class, root.get("categories"), hcb.value(normalized)));
        };
    }

    private static String containsPattern(String input) {
        String escaped = input.strip()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }
}
