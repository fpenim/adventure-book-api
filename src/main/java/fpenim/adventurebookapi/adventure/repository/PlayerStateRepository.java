package fpenim.adventurebookapi.adventure.repository;

import static fpenim.adventurebookapi.jooq.Tables.PLAYER_STATES;
import static org.jooq.Records.mapping;

import fpenim.adventurebookapi.adventure.model.PlayerState;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerStateRepository {

    private final DSLContext dsl;

    public PlayerStateRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<PlayerState> find(String username, Long bookId) {
        return select(username, bookId).fetchOptional(mapping(PlayerState::new));
    }

    /** Locks the row until the transaction ends, so concurrent requests by one player apply one at a time. */
    public Optional<PlayerState> findForUpdate(String username, Long bookId) {
        return select(username, bookId).forUpdate().fetchOptional(mapping(PlayerState::new));
    }

    /** Returns false when the player already has a row for the book. */
    public boolean insertIfAbsent(PlayerState state) {
        return dsl.insertInto(PLAYER_STATES)
                        .set(PLAYER_STATES.USERNAME, state.username())
                        .set(PLAYER_STATES.BOOK_ID, state.bookId())
                        .set(PLAYER_STATES.CURRENT_SECTION_ID, state.sectionId())
                        .set(PLAYER_STATES.HEALTH, state.health())
                        .onConflictDoNothing()
                        .execute()
                > 0;
    }

    public void update(PlayerState state) {
        dsl.update(PLAYER_STATES)
                .set(PLAYER_STATES.CURRENT_SECTION_ID, state.sectionId())
                .set(PLAYER_STATES.HEALTH, state.health())
                .where(PLAYER_STATES.USERNAME.eq(state.username()))
                .and(PLAYER_STATES.BOOK_ID.eq(state.bookId()))
                .execute();
    }

    private SelectConditionStep<Record4<String, Long, Integer, Integer>> select(String username, Long bookId) {
        return dsl.select(
                        PLAYER_STATES.USERNAME,
                        PLAYER_STATES.BOOK_ID,
                        PLAYER_STATES.CURRENT_SECTION_ID,
                        PLAYER_STATES.HEALTH)
                .from(PLAYER_STATES)
                .where(PLAYER_STATES.USERNAME.eq(username))
                .and(PLAYER_STATES.BOOK_ID.eq(bookId));
    }
}
