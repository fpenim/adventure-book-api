package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.model.Difficulty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookSummaryResponse> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Difficulty difficulty) {
        return bookService.findBooks(new BookFilter(title, author, category, difficulty));
    }

    @GetMapping(path = "/{id}")
    public BookSummaryResponse getBook(@PathVariable Long id) {
        return bookService.findBookById(id);
    }

    @PutMapping(path = "/{id}/categories/{category}")
    public void addBookCategory(@PathVariable Long id, @PathVariable String category) {
        bookService.addBookCategory(id, category);
    }

    @DeleteMapping(path = "/{id}/categories/{category}")
    public void removeBookCategory(@PathVariable Long id, @PathVariable String category) {
        bookService.removeBookCategory(id, category);
    }
}
