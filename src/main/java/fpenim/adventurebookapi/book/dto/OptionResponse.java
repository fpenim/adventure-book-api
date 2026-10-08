package fpenim.adventurebookapi.book.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class OptionResponse extends RepresentationModel<OptionResponse> {

    private final String description;
    private final int gotoId;
    private final List<Consequence> consequences;

    public OptionResponse(String description, int gotoId, List<Consequence> consequences) {
        this.description = description;
        this.gotoId = gotoId;
        this.consequences = consequences;
    }

    public String getDescription() {
        return description;
    }

    @JsonIgnore
    public int getGotoId() {
        return gotoId;
    }

    @JsonInclude(NON_EMPTY)
    public List<Consequence> getConsequences() {
        return consequences;
    }
}
