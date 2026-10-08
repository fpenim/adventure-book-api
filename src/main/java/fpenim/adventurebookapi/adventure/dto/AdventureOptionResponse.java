package fpenim.adventurebookapi.adventure.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import java.util.List;

public record AdventureOptionResponse(
        int option,
        String description,
        @JsonInclude(NON_EMPTY) List<Consequence> consequences) {}
