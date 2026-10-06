package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.repository.BookEntity;
import fpenim.adventurebookapi.book.repository.BookRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BookImportService {

    private final BookImportValidator bookImportValidator;

    private final Validator validator;

    private final BookRepository bookRepository;

    public BookImportService(
            BookImportValidator bookImportValidator, Validator validator, BookRepository bookRepository) {
        this.bookImportValidator = bookImportValidator;
        this.validator = validator;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public void importBook(BookImportRequest book) {
        try {
            Set<ConstraintViolation<BookImportRequest>> violations = validator.validate(book);

            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            bookImportValidator.validateStructure(book);
        } catch (ValidationException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid book: " + book.title() + ":" + e.getMessage(), e);
        }

        try {
            bookRepository.save(
                    new BookEntity(book.title(), book.author(), book.difficulty(), book.categories(), book.sections()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
