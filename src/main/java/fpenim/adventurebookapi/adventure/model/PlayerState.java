package fpenim.adventurebookapi.adventure.model;

public record PlayerState(String username, Long bookId, int sectionId, int health) {}
