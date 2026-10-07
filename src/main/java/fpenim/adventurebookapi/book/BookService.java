package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.dto.OptionResponse;
import fpenim.adventurebookapi.book.dto.SectionResponse;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.repository.BookEntity;
import fpenim.adventurebookapi.book.repository.BookRepository;
import fpenim.adventurebookapi.book.repository.ConsequenceEntity;
import fpenim.adventurebookapi.book.repository.SectionEntity;
import fpenim.adventurebookapi.book.repository.SectionRepository;
import java.util.List;
import java.util.Optional;
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
        return bookRepository.findAll(BookSpecifications.matches(filter)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookSummaryResponse findBookById(Long id) {
        return bookRepository.findById(id).map(this::toResponse).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public void addBookCategory(Long id, String category) {
        BookEntity book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));

        book.addCategory(category);
    }

    @Transactional
    public void removeBookCategory(Long id, String category) {
        BookEntity book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));

        book.removeCategory(category);
    }

    @Transactional(readOnly = true)
    public SectionResponse findSectionInBookById(Long bookId, int sectionId) {
        SectionEntity section = sectionRepository
                .findById(new SectionEntity.SectionId(bookId, sectionId))
                .orElseThrow(() -> new SectionNotFoundException(sectionId, bookId));

        List<OptionResponse> options = section.getOptions().stream()
                .map(option -> new OptionResponse(
                        option.getDescription(),
                        option.getGoTo(),
                        option.getConsequences().stream()
                                .map(this::toConsequence)
                                .toList()))
                .toList();

        return new SectionResponse(sectionId, section.getText(), section.getType(), options);
    }

    private BookSummaryResponse toResponse(BookEntity bookEntity) {
        Optional<SectionEntity> beginSection = bookEntity.getSections().stream()
                .filter(s -> s.getType() == SectionType.BEGIN)
                .findFirst();

        if (beginSection.isPresent()) {
            SectionEntity section = beginSection.get();

            return new BookSummaryResponse(
                    bookEntity.getId(),
                    bookEntity.getTitle(),
                    bookEntity.getAuthor(),
                    bookEntity.getCategories(),
                    bookEntity.getDifficulty(),
                    section.getId());
        }

        throw new IllegalStateException("No begin section found for book [" + bookEntity.getId() + "].");
    }

    private Consequence toConsequence(ConsequenceEntity entity) {
        return new Consequence(entity.getType(), entity.getValue(), entity.getText());
    }
}
