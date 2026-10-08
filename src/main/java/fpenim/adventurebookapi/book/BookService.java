package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.dto.SectionResponse;
import fpenim.adventurebookapi.book.repository.BookRepository;
import fpenim.adventurebookapi.book.repository.SectionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final SectionRepository sectionRepository;

    public BookService(BookRepository bookRepository, SectionRepository sectionRepository) {
        this.bookRepository = bookRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<BookSummaryResponse> findBooks(BookFilter filter) {
        return bookRepository.findAll(filter);
    }

    @Transactional(readOnly = true)
    public BookSummaryResponse findBookById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public void addBookCategory(Long id, String category) {
        if (category.isBlank()) {
            throw new IllegalArgumentException("Category must not be blank.");
        }

        if (!bookRepository.addCategory(id, category)) {
            throw new BookNotFoundException(id);
        }
    }

    @Transactional
    public void removeBookCategory(Long id, String category) {
        if (!bookRepository.removeCategory(id, category)) {
            throw new BookNotFoundException(id);
        }
    }

    @Transactional(readOnly = true)
    public SectionResponse findSectionInBookById(Long bookId, int sectionId) {
        return sectionRepository
                .findSection(bookId, sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId, bookId));
    }
}
