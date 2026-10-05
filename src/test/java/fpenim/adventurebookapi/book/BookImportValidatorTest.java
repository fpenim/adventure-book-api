package fpenim.adventurebookapi.book;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Option;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookImportValidatorTest {

    private final BookImportValidator validator = new BookImportValidator();

    @Test
    void acceptsValidBook() {
        BookImportRequest book =
                book(section(1, SectionType.BEGIN, 2, 3), section(2, SectionType.NODE, 3), section(3, SectionType.END));

        assertThatCode(() -> validator.validateStructure(book)).doesNotThrowAnyException();
    }

    @Test
    void acceptsOptionsThatLoopBackToAnEarlierSection() {
        BookImportRequest book =
                book(section(1, SectionType.BEGIN, 2), section(2, SectionType.NODE, 1, 3), section(3, SectionType.END));

        assertThatCode(() -> validator.validateStructure(book)).doesNotThrowAnyException();
    }

    @Test
    void acceptsMultipleEndSections() {
        BookImportRequest book =
                book(section(1, SectionType.BEGIN, 2, 3), section(2, SectionType.END), section(3, SectionType.END));

        assertThatCode(() -> validator.validateStructure(book)).doesNotThrowAnyException();
    }

    @Test
    void acceptsSectionThatNoOptionLeadsTo() {
        BookImportRequest book =
                book(section(1, SectionType.BEGIN, 3), section(2, SectionType.NODE, 3), section(3, SectionType.END));

        assertThatCode(() -> validator.validateStructure(book)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateSectionId() {
        BookImportRequest book = book(
                section(1, SectionType.BEGIN, 2),
                section(2, SectionType.NODE, 3),
                section(2, SectionType.NODE, 3),
                section(3, SectionType.END));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate section ID: 2");
    }

    @Test
    void rejectsEndSectionWithOptions() {
        BookImportRequest book = book(section(1, SectionType.BEGIN, 2), section(2, SectionType.END, 1));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An END section should not have options.");
    }

    @Test
    void rejectsBeginSectionWithoutOptions() {
        BookImportRequest book = book(section(1, SectionType.BEGIN), section(2, SectionType.END));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A BEGIN or NODE section should have options.");
    }

    @Test
    void rejectsNodeSectionWithoutOptions() {
        BookImportRequest book =
                book(section(1, SectionType.BEGIN, 2), section(2, SectionType.NODE), section(3, SectionType.END));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A BEGIN or NODE section should have options.");
    }

    @Test
    void rejectsBookWithoutBeginSection() {
        BookImportRequest book = book(section(1, SectionType.NODE, 2), section(2, SectionType.END));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected 1 BEGIN section but got: 0");
    }

    @Test
    void rejectsBookWithMultipleBeginSections() {
        BookImportRequest book =
                book(section(1, SectionType.BEGIN, 3), section(2, SectionType.BEGIN, 3), section(3, SectionType.END));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected 1 BEGIN section but got: 2");
    }

    @Test
    void rejectsBookWithoutEndSection() {
        BookImportRequest book = book(section(1, SectionType.BEGIN, 2), section(2, SectionType.NODE, 1));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected at least 1 END section but got: 0");
    }

    @Test
    void rejectsOptionPointingAtMissingSection() {
        BookImportRequest book = book(section(1, SectionType.BEGIN, 99), section(2, SectionType.END));

        assertThatThrownBy(() -> validator.validateStructure(book))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Section 1 references missing section 99");
    }

    private static BookImportRequest book(Section... sections) {
        return new BookImportRequest("Test Book", "Test Author", List.of(), Difficulty.EASY, List.of(sections));
    }

    private static Section section(int id, SectionType type, int... gotoIds) {
        List<Option> options = Arrays.stream(gotoIds)
                .mapToObj(gotoId -> new Option("Go to " + gotoId, gotoId, List.of()))
                .toList();

        return new Section(id, "Section " + id, type, options);
    }
}
