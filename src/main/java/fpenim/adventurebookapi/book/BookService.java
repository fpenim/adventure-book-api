package fpenim.adventurebookapi.book;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final List<Book> books = List.of(
            new Book(
                    "The Lost Kingdom", "Jane Doe", List.of("Fantasy"),
                    Difficulty.EASY, List.of()
            ),
            new Book(
                    "The Hidden Forest", "John Smith", List.of("Fantasy", "Adventure"),
                    Difficulty.HARD, List.of()
            ),
            new Book(
                    "The Missing Key", "Jane Doe", List.of(),
                    Difficulty.EASY, List.of()
            )
    );

    public List<Book> findBooks(
            String title,
            String author,
            String category,
            Difficulty difficulty) {

        return books.stream()
                .filter(book -> containsIgnoreCase(book.title(), title))
                .filter(book -> containsIgnoreCase(book.author(), author))
                .filter(book -> matchesCategory(book.categories(), category))
                .filter(book -> difficulty == null || book.difficulty() == difficulty)
                .toList();
    }

    private boolean containsIgnoreCase(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        return value != null
                && value.toLowerCase()
                .contains(filter.strip().toLowerCase());
    }

    private boolean matchesCategory(List<String> categories, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        return categories != null
                && categories.stream()
                .anyMatch(category -> category != null && category.equalsIgnoreCase(filter.strip()));
    }

}
