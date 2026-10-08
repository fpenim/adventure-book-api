package fpenim.adventurebookapi.adventure.model;

public record Adventure(Long id, String username, Long bookId, int currentSectionId, int health) {}
