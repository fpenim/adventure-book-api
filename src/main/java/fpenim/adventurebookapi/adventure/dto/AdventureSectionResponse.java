package fpenim.adventurebookapi.adventure.dto;

import fpenim.adventurebookapi.book.model.section.SectionType;
import java.util.List;

public record AdventureSectionResponse(int id, String text, SectionType type, List<AdventureOptionResponse> options) {}
