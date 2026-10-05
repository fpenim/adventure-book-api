package fpenim.adventurebookapi.book;

import fpenim.adventurebookapi.book.dto.BookImportRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class BookImportValidator {

    public void validateStructure(BookImportRequest book) {
        Set<Integer> sectionIds = new HashSet<>();

        for (var section : book.sections()) {
            if (!sectionIds.add(section.id())) {
                throw new IllegalArgumentException("Duplicate section ID: " + section.id());
            }
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
