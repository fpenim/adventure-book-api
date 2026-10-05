package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class BookImportValidator {

    public void validateStructure(BookImportRequest book) {
        Set<Integer> sectionIds = new HashSet<>();
        int begingCount = 0;
        int endCounter = 0;

        for (var section : book.sections()) {
            if (!sectionIds.add(section.id())) {
                throw new IllegalArgumentException("Duplicate section ID: " + section.id());
            }

            if (section.type() == SectionType.BEGIN) {
                begingCount++;
            }
            if (section.type() == SectionType.END) {
                endCounter++;
                if (!section.options().isEmpty()) {
                    throw new IllegalArgumentException("An END section should not have options.");
                }
            }
            if (section.type() != SectionType.END && section.options().isEmpty()) {
                throw new IllegalArgumentException("A BEGIN or NODE section should have options.");
            }
        }

        if (begingCount != 1) {
            throw new IllegalArgumentException("Expected 1 BEGIN section but got: " + begingCount);
        }

        if (endCounter < 1) {
            throw new IllegalArgumentException("Expected at least 1 END section but got: " + endCounter);
        }

        for (var section : book.sections()) {
            for (var option : section.options()) {
                if (!sectionIds.contains(option.gotoId())) {
                    throw new IllegalArgumentException(
                            "Section " + section.id() + " references missing section " + option.gotoId()
                    );
                }
            }
        }


    }
}
