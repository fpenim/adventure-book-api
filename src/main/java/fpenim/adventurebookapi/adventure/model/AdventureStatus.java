package fpenim.adventurebookapi.adventure.model;

import fpenim.adventurebookapi.book.model.section.SectionType;

public enum AdventureStatus {
    IN_PROGRESS,
    DEAD,
    FINISHED;

    // Not stored: it follows from the health and the section the player is on.
    public static AdventureStatus of(int health, SectionType sectionType) {
        if (health <= 0) {
            return DEAD;
        }

        return sectionType == SectionType.END ? FINISHED : IN_PROGRESS;
    }
}
