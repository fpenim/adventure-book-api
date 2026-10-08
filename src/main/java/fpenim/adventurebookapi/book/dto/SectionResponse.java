package fpenim.adventurebookapi.book.dto;

import fpenim.adventurebookapi.book.model.section.SectionType;
import java.util.List;

public record SectionResponse(int id, String text, SectionType type, List<OptionResponse> options) {}
