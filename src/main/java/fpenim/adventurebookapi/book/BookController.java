package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.model.Difficulty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
        BookSummaryResponse book = bookService.findBookById(id);

        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return book;
    }

    @PutMapping(path = "/{id}/categories/{category}")
    public void addBookCategory(
            @PathVariable
            Long id,
            @PathVariable
            String category
    ) {
        try {
            bookService.addBookCategory(id, category);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping(path = "/{id}/categories/{category}")
    public void removeBookCategory(
            @PathVariable Long id,
            @PathVariable String category
    ) {
        try {
            bookService.removeBookCategory(id, category);
        }  catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
