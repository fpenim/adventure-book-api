package fpenim.adventurebookapi.adventure;

public class AdventureNotFoundException extends RuntimeException {

    public AdventureNotFoundException(Long id) {
        super("Adventure with id: [" + id + "] not found.");
    }
}
