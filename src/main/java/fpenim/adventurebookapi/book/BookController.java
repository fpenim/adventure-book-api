package fpenim.adventurebookapi.book;

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
    public List<Book> getBooks(
            @RequestParam(name = "title", required = false)
            String title,

            @RequestParam(name = "author", required = false)
            String author,

            @RequestParam(name = "category", required = false)
            String category,

            // For enum parameters, use the exact constant name, such as EASY.
            // Spring’s default enum conversion is case-sensitive; an unrecognized value produces 400 Bad Request.
            @RequestParam(name = "difficulty", required = false)
            Difficulty difficulty
    ) {
        return bookService.findBooks(title, author, category, difficulty);
    }
}
