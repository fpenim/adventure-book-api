package fpenim.adventurebookapi.book;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import fpenim.adventurebookapi.adventure.AdventureController;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.dto.SectionResponse;
import fpenim.adventurebookapi.book.model.Difficulty;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final BookImportService bookImportService;

    public BookController(BookService bookService, BookImportService bookImportService) {
        this.bookService = bookService;
        this.bookImportService = bookImportService;
    }

    @PostMapping
    public ResponseEntity<EntityModel<BookSummaryResponse>> addBook(@RequestBody BookImportRequest request) {
        Long id = bookImportService.importBook(request);

        return ResponseEntity.created(
                        linkTo(methodOn(BookController.class).getBook(id)).toUri())
                .body(getBook(id));
    }

    @GetMapping
    public CollectionModel<EntityModel<BookSummaryResponse>> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Difficulty difficulty) {

        List<EntityModel<BookSummaryResponse>> bookList =
                bookService.findBooks(new BookFilter(title, author, category, difficulty)).stream()
                        .map(book -> EntityModel.of(
                                book,
                                linkTo(methodOn(BookController.class).getBook(book.id()))
                                        .withSelfRel(),
                                linkTo(methodOn(BookController.class).getSection(book.id(), book.beginSectionId()))
                                        .withRel("begin"),
                                linkTo(methodOn(AdventureController.class).startAdventure(null, null))
                                        .withRel("start")))
                        .toList();

        return CollectionModel.of(bookList);
    }

    @GetMapping(path = "/{id}")
    public EntityModel<BookSummaryResponse> getBook(@PathVariable Long id) {
        BookSummaryResponse book = bookService.findBookById(id);

        return EntityModel.of(
                book,
                linkTo(methodOn(BookController.class).getSection(id, book.beginSectionId()))
                        .withRel("begin"),
                linkTo(methodOn(AdventureController.class).startAdventure(null, null))
                        .withRel("start"));
    }

    @GetMapping(path = "/{id}/sections/{sectionId}")
    public EntityModel<SectionResponse> getSection(@PathVariable Long id, @PathVariable Integer sectionId) {
        SectionResponse sectionResponse = bookService.findSectionInBookById(id, sectionId);

        sectionResponse
                .options()
                .forEach(option ->
                        option.add(linkTo(methodOn(BookController.class).getSection(id, option.getGotoId()))
                                .withRel("goTo")));

        return EntityModel.of(sectionResponse);
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
