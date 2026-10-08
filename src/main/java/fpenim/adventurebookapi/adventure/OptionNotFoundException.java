package fpenim.adventurebookapi.adventure;

public class OptionNotFoundException extends RuntimeException {
    public OptionNotFoundException(int position, int sectionId) {
        super("Option [" + position + "] on section [" + sectionId + "] not found.");
    }
}
