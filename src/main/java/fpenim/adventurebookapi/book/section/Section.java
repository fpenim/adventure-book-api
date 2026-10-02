package fpenim.adventurebookapi.book.section;

import fpenim.adventurebookapi.book.section.option.Option;

import java.util.ArrayList;
import java.util.List;

public record Section(int id, String text, SectionType sectionType, List<Option> options) {}
