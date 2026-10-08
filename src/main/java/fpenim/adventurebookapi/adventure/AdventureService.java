package fpenim.adventurebookapi.adventure;

import fpenim.adventurebookapi.adventure.dto.AdventureResponse;
import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import fpenim.adventurebookapi.adventure.model.PlayerState;
import fpenim.adventurebookapi.adventure.repository.PlayerStateRepository;
import fpenim.adventurebookapi.book.BookNotFoundException;
import fpenim.adventurebookapi.book.SectionNotFoundException;
import fpenim.adventurebookapi.book.dto.OptionResponse;
import fpenim.adventurebookapi.book.dto.SectionResponse;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.repository.BookRepository;
import fpenim.adventurebookapi.book.repository.SectionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdventureService {

    static final int STARTING_HEALTH = 10;

    private final PlayerStateRepository playerStateRepository;
    private final BookRepository bookRepository;
    private final SectionRepository sectionRepository;

    public AdventureService(
            PlayerStateRepository playerStateRepository,
            BookRepository bookRepository,
            SectionRepository sectionRepository) {
        this.playerStateRepository = playerStateRepository;
        this.bookRepository = bookRepository;
        this.sectionRepository = sectionRepository;
    }

    public record Started(AdventureResponse adventure, boolean created) {}

    /** Starts the book from its BEGIN section, or returns the existing progress unchanged if already started. */
    @Transactional
    public Started start(String username, Long bookId) {
        String player = normalize(username);
        PlayerState fresh = freshState(player, bookId);

        if (playerStateRepository.insertIfAbsent(fresh)) {
            return new Started(toResponse(fresh), true);
        }
        return new Started(toResponse(findState(player, bookId, false)), false);
    }

    @Transactional(readOnly = true)
    public AdventureResponse current(String username, Long bookId) {
        return toResponse(findState(normalize(username), bookId, false));
    }

    /**
     * Moves the player along the chosen option and applies its consequences. The option is identified by its index in
     * the section's options, and the section must be the one the player is currently on, so a repeated or stale request
     * is rejected instead of being applied as a second move.
     */
    @Transactional
    public AdventureResponse choose(String username, Long bookId, int sectionId, int optionPosition) {
        String player = normalize(username);
        PlayerState state = findState(player, bookId, true);
        SectionResponse section = findSection(bookId, state.sectionId());

        switch (statusOf(state.health(), section)) {
            case DEAD -> throw new AdventureConflictException("Player [" + player + "] has no health left.");
            case FINISHED -> throw new AdventureConflictException("Player [" + player + "] has finished the book.");
            case IN_PROGRESS -> {}
        }
        if (state.sectionId() != sectionId) {
            throw new AdventureConflictException(
                    "Player [" + player + "] is on section [" + state.sectionId() + "], not [" + sectionId + "].");
        }

        List<OptionResponse> options = section.options();
        if (optionPosition < 0 || optionPosition >= options.size()) {
            throw new OptionNotFoundException(optionPosition, sectionId);
        }
        OptionResponse option = options.get(optionPosition);

        PlayerState moved = new PlayerState(
                player, bookId, option.getGotoId(), applyConsequences(state.health(), option.getConsequences()));
        playerStateRepository.update(moved);

        return toResponse(moved);
    }

    /** Puts the player back on the BEGIN section with full health, whatever state the adventure is in. */
    @Transactional
    public AdventureResponse restart(String username, Long bookId) {
        String player = normalize(username);
        findState(player, bookId, true);

        PlayerState fresh = freshState(player, bookId);
        playerStateRepository.update(fresh);

        return toResponse(fresh);
    }

    private PlayerState freshState(String player, Long bookId) {
        int beginSectionId = bookRepository
                .findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId))
                .beginSectionId();
        return new PlayerState(player, bookId, beginSectionId, STARTING_HEALTH);
    }

    private PlayerState findState(String player, Long bookId, boolean forUpdate) {
        var state = forUpdate
                ? playerStateRepository.findForUpdate(player, bookId)
                : playerStateRepository.find(player, bookId);
        return state.orElseThrow(() -> new AdventureNotFoundException(player, bookId));
    }

    private static int applyConsequences(int health, List<Consequence> consequences) {
        for (Consequence consequence : consequences) {
            health += switch (consequence.type()) {
                case GAIN_HEALTH -> consequence.value();
                case LOSE_HEALTH -> -consequence.value();
            };
        }
        return Math.max(health, 0);
    }

    private AdventureResponse toResponse(PlayerState state) {
        SectionResponse section = findSection(state.bookId(), state.sectionId());
        return new AdventureResponse(
                state.username(), state.bookId(), state.health(), statusOf(state.health(), section), section);
    }

    private SectionResponse findSection(Long bookId, int sectionId) {
        return sectionRepository
                .findSection(bookId, sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId, bookId));
    }

    private static AdventureStatus statusOf(int health, SectionResponse section) {
        if (health <= 0) {
            return AdventureStatus.DEAD;
        }
        return section.type() == SectionType.END ? AdventureStatus.FINISHED : AdventureStatus.IN_PROGRESS;
    }

    private static String normalize(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        return username.strip();
    }
}
