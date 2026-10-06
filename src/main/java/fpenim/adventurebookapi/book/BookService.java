package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.repository.BookEntity;
import fpenim.adventurebookapi.book.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<BookSummaryResponse> findBooks(BookFilter filter) {
        return bookRepository.findAll(BookSpecifications.matches(filter)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookSummaryResponse findBookById(Long id) {
        return bookRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public void addBookCategory(Long id, String category) {
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        book.addCategory(category);
    }

    @Transactional
    public void removeBookCategory(Long id, String category) {
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        book.removeCategory(category);
    }

    private BookSummaryResponse toResponse(BookEntity bookEntity) {
        return new BookSummaryResponse(
                bookEntity.getId(),
                bookEntity.getTitle(),
                bookEntity.getAuthor(),
                bookEntity.getCategories(),
                bookEntity.getDifficulty());
    }
}
