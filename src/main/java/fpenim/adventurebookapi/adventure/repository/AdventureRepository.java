package fpenim.adventurebookapi.adventure.repository;

import static fpenim.adventurebookapi.jooq.Tables.ADVENTURES;
import static fpenim.adventurebookapi.jooq.Tables.BOOKS;
import static fpenim.adventurebookapi.jooq.Tables.SECTIONS;
import static org.jooq.Records.mapping;

import fpenim.adventurebookapi.adventure.dto.AdventureSummaryResponse;
import fpenim.adventurebookapi.adventure.model.Adventure;
import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

@Repository
public class AdventureRepository {

    private final DSLContext dsl;

    public AdventureRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Long insert(String username, Long bookId, int sectionId, int health) {
        return dsl.insertInto(ADVENTURES)
                .set(ADVENTURES.USERNAME, username)
                .set(ADVENTURES.BOOK_ID, bookId)
                .set(ADVENTURES.CURRENT_SECTION_ID, sectionId)
                .set(ADVENTURES.HEALTH, health)
                .returningResult(ADVENTURES.ID)
                .fetchSingle(ADVENTURES.ID);
    }

    public Optional<Adventure> findByIdAndUsername(Long id, String username) {
        return selectAdventure(id, username).fetchOptional(mapping(Adventure::new));
    }

    public Optional<Adventure> findByIdAndUsernameForUpdate(Long id, String username) {
        return selectAdventure(id, username).forUpdate().fetchOptional(mapping(Adventure::new));
    }

    public List<AdventureSummaryResponse> findAllByUsername(String username) {
        return dsl.select(
                        ADVENTURES.ID,
                        ADVENTURES.BOOK_ID,
                        BOOKS.TITLE,
                        ADVENTURES.CURRENT_SECTION_ID,
                        ADVENTURES.HEALTH,
                        SECTIONS.SECTION_TYPE)
                .from(ADVENTURES)
                .join(BOOKS)
                .on(BOOKS.ID.eq(ADVENTURES.BOOK_ID))
                .join(SECTIONS)
                .on(SECTIONS.BOOK_ID.eq(ADVENTURES.BOOK_ID))
                .and(SECTIONS.ID.eq(ADVENTURES.CURRENT_SECTION_ID))
                .where(ADVENTURES.USERNAME.eq(username))
                .orderBy(ADVENTURES.ID)
                .fetch(row -> new AdventureSummaryResponse(
                        row.value1(),
                        row.value2(),
                        row.value3(),
                        row.value4(),
                        row.value5(),
                        AdventureStatus.of(row.value5(), row.value6())));
    }

    public void updateState(Long id, int sectionId, int health) {
        dsl.update(ADVENTURES)
                .set(ADVENTURES.CURRENT_SECTION_ID, sectionId)
                .set(ADVENTURES.HEALTH, health)
                .where(ADVENTURES.ID.eq(id))
                .execute();
    }

    public boolean delete(Long id, String username) {
        return dsl.deleteFrom(ADVENTURES)
                        .where(ADVENTURES.ID.eq(id))
                        .and(ADVENTURES.USERNAME.eq(username))
                        .execute()
                > 0;
    }

    private SelectConditionStep<Record5<Long, String, Long, Integer, Integer>> selectAdventure(
            Long id, String username) {
        return dsl.select(
                        ADVENTURES.ID,
                        ADVENTURES.USERNAME,
                        ADVENTURES.BOOK_ID,
                        ADVENTURES.CURRENT_SECTION_ID,
                        ADVENTURES.HEALTH)
                .from(ADVENTURES)
                .where(ADVENTURES.ID.eq(id))
                .and(ADVENTURES.USERNAME.eq(username));
    }
}
