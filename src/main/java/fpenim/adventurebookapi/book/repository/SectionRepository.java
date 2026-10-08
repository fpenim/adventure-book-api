package fpenim.adventurebookapi.book.repository;

import static fpenim.adventurebookapi.jooq.Tables.CONSEQUENCES;
import static fpenim.adventurebookapi.jooq.Tables.OPTIONS;
import static fpenim.adventurebookapi.jooq.Tables.SECTIONS;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

import fpenim.adventurebookapi.book.dto.OptionResponse;
import fpenim.adventurebookapi.book.dto.SectionResponse;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

@Repository
public class SectionRepository {

    private final DSLContext dsl;

    public SectionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<SectionResponse> findSection(Long bookId, int sectionId) {
        Field<List<Consequence>> consequences = multiset(
                        select(CONSEQUENCES.TYPE, CONSEQUENCES.VALUE, CONSEQUENCES.TEXT)
                                .from(CONSEQUENCES)
                                .where(CONSEQUENCES.BOOK_ID.eq(OPTIONS.BOOK_ID))
                                .and(CONSEQUENCES.SECTION_ID.eq(OPTIONS.SECTION_ID))
                                .and(CONSEQUENCES.OPTION_POSITION.eq(OPTIONS.POSITION))
                                .orderBy(CONSEQUENCES.POSITION))
                .convertFrom(result -> result.map(mapping(Consequence::new)));

        Field<List<OptionResponse>> options = multiset(select(OPTIONS.DESCRIPTION, OPTIONS.GO_TO, consequences)
                        .from(OPTIONS)
                        .where(OPTIONS.BOOK_ID.eq(SECTIONS.BOOK_ID))
                        .and(OPTIONS.SECTION_ID.eq(SECTIONS.ID))
                        .orderBy(OPTIONS.POSITION))
                .convertFrom(result -> result.map(mapping(OptionResponse::new)));

        return dsl.select(SECTIONS.ID, SECTIONS.TEXT, SECTIONS.SECTION_TYPE, options)
                .from(SECTIONS)
                .where(SECTIONS.BOOK_ID.eq(bookId))
                .and(SECTIONS.ID.eq(sectionId))
                .fetchOptional(mapping(SectionResponse::new));
    }
}
