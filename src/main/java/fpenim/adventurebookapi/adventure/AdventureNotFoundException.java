package fpenim.adventurebookapi.adventure;

public class AdventureNotFoundException extends RuntimeException {
    public AdventureNotFoundException(String username, Long bookId) {
        super("Player [" + username + "] has not started book id [" + bookId + "].");
    }
}
