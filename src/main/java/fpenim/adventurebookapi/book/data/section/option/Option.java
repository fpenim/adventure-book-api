package fpenim.adventurebookapi.book.data.section.option;

import java.util.List;

public record Option(
        String description,
        int goTo,
        List<Consequence> consequences
) {}
