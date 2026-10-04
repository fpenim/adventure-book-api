package fpenim.adventurebookapi.book.data.section;

import fpenim.adventurebookapi.book.data.section.option.Option;

import java.util.List;

public record Section(int id, String text, SectionType sectionType, List<Option> options) {}
