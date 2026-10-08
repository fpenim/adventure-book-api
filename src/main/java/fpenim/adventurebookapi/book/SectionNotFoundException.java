package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.model.section.SectionType;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(int id, Long bookId) {
        super("Section with id: [" + id + "] on book id [" + bookId + "] not found.");
    }

    public SectionNotFoundException(SectionType type, Long bookId) {
        super(type + " section on book id [" + bookId + "] not found.");
    }
}
