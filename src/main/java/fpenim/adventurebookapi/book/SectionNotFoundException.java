package fpenim.adventurebookapi.book;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(int id, Long bookId) {
        super("Section with id: [" + id + "] on book id [" + bookId + "] not found.");
    }
}
