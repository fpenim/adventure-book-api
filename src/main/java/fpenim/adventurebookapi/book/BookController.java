package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.data.Difficulty;
import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(required = false)
            String title,

            @RequestParam(required = false)
            String author,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            Difficulty difficulty
    ) {
        return bookService.findBooks(new BookFilter(title, author, category, difficulty));
    }
}
