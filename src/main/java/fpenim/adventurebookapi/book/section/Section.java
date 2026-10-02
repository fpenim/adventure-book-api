package fpenim.adventurebookapi.book.section;

import fpenim.adventurebookapi.book.section.option.Option;

import java.util.ArrayList;

public record Section(int id, String text, SectionType sectionType, ArrayList<Option> options) {}
