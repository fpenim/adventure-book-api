package fpenim.adventurebookapi.book.model.section.option;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record Option(
        @NotBlank String description,

        @Positive int gotoId,

        @JsonProperty("consequence") @JsonAlias("consequences") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<@Valid Consequence> consequences) {
    public Option {
        consequences = consequences == null ? List.of() : List.copyOf(consequences);
    }
}
