package fpenim.adventurebookapi.adventure;

public class InvalidOptionException extends RuntimeException {

    public InvalidOptionException(int option, int sectionId) {
        super("Option: [" + option + "] does not exist on section id [" + sectionId + "].");
    }
}
