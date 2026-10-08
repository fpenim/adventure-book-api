package fpenim.adventurebookapi.adventure;

import fpenim.adventurebookapi.adventure.model.AdventureStatus;

public class AdventureOverException extends RuntimeException {

    public AdventureOverException(Long id, AdventureStatus status) {
        super("Adventure with id: [" + id + "] is over: " + status + ".");
    }
}
