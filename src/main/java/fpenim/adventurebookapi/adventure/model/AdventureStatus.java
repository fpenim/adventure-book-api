package fpenim.adventurebookapi.adventure.model;

import fpenim.adventurebookapi.book.model.section.SectionType;

public enum AdventureStatus {
    IN_PROGRESS,
    DEAD,
    FINISHED;

    public static AdventureStatus of(int health, SectionType sectionType) {
        if (health <= 0) {
            return DEAD;
        }

        return sectionType == SectionType.END ? FINISHED : IN_PROGRESS;
    }
}
