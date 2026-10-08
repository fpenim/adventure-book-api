package fpenim.adventurebookapi.adventure;

import fpenim.adventurebookapi.adventure.dto.AdventureOptionResponse;
import fpenim.adventurebookapi.adventure.dto.AdventureResponse;
import fpenim.adventurebookapi.adventure.dto.AdventureSectionResponse;
import fpenim.adventurebookapi.adventure.dto.AdventureSummaryResponse;
import fpenim.adventurebookapi.adventure.model.Adventure;
import fpenim.adventurebookapi.adventure.model.AdventureStatus;
import fpenim.adventurebookapi.adventure.repository.AdventureRepository;
import fpenim.adventurebookapi.book.BookNotFoundException;
import fpenim.adventurebookapi.book.SectionNotFoundException;
import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.dto.OptionResponse;
import fpenim.adventurebookapi.book.dto.SectionResponse;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.repository.BookRepository;
import fpenim.adventurebookapi.book.repository.SectionRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdventureService {

    static final int STARTING_HEALTH = 10;

    private final AdventureRepository adventureRepository;
    private final BookRepository bookRepository;
    private final SectionRepository sectionRepository;

    public AdventureService(
            AdventureRepository adventureRepository,
            BookRepository bookRepository,
            SectionRepository sectionRepository) {
        this.adventureRepository = adventureRepository;
        this.bookRepository = bookRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional
    public AdventureResponse startAdventure(String username, Long bookId) {
        String player = requireUsername(username);
        BookSummaryResponse book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));

        Long id = adventureRepository.insert(player, bookId, book.beginSectionId(), STARTING_HEALTH);

        return toResponse(new Adventure(id, player, bookId, book.beginSectionId(), STARTING_HEALTH));
    }

    @Transactional(readOnly = true)
    public List<AdventureSummaryResponse> findAdventures(String username) {
        return adventureRepository.findAllByUsername(requireUsername(username));
    }

    @Transactional(readOnly = true)
    public AdventureResponse findAdventure(String username, Long id) {
        return adventureRepository
                .findByIdAndUsername(id, requireUsername(username))
                .map(this::toResponse)
                .orElseThrow(() -> new AdventureNotFoundException(id));
    }

    @Transactional
    public AdventureResponse move(String username, Long id, int option) {
        Adventure adventure = adventureRepository
                .findByIdAndUsernameForUpdate(id, requireUsername(username))
                .orElseThrow(() -> new AdventureNotFoundException(id));
        SectionResponse section = findSection(adventure.bookId(), adventure.currentSectionId());

        AdventureStatus status = AdventureStatus.of(adventure.health(), section.type());
        if (status != AdventureStatus.IN_PROGRESS) {
            throw new AdventureOverException(id, status);
        }

        if (option < 0 || option >= section.options().size()) {
            throw new InvalidOptionException(option, section.id());
        }

        OptionResponse chosen = section.options().get(option);
        int health = applyConsequences(adventure.health(), chosen.getConsequences());
        adventureRepository.updateState(id, chosen.getGotoId(), health);

        return toResponse(new Adventure(id, adventure.username(), adventure.bookId(), chosen.getGotoId(), health));
    }

    @Transactional
    public void deleteAdventure(String username, Long id) {
        if (!adventureRepository.delete(id, requireUsername(username))) {
            throw new AdventureNotFoundException(id);
        }
    }

    // Applied in order. Health never goes below zero, and nothing heals a player who has died.
    private static int applyConsequences(int health, List<Consequence> consequences) {
        for (Consequence consequence : consequences) {
            health = switch (consequence.type()) {
                case GAIN_HEALTH -> health + consequence.value();
                case LOSE_HEALTH -> Math.max(0, health - consequence.value());
            };

            if (health == 0) {
                break;
            }
        }

        return health;
    }

    private AdventureResponse toResponse(Adventure adventure) {
        SectionResponse section = findSection(adventure.bookId(), adventure.currentSectionId());

        // Options come back in stored order, so the index is the option's position.
        List<OptionResponse> options = section.options();
        List<AdventureOptionResponse> numberedOptions = IntStream.range(0, options.size())
                .mapToObj(position -> new AdventureOptionResponse(
                        position,
                        options.get(position).getDescription(),
                        options.get(position).getConsequences()))
                .toList();

        return new AdventureResponse(
                adventure.id(),
                adventure.username(),
                adventure.bookId(),
                adventure.health(),
                AdventureStatus.of(adventure.health(), section.type()),
                new AdventureSectionResponse(section.id(), section.text(), section.type(), numberedOptions));
    }

    private SectionResponse findSection(Long bookId, int sectionId) {
        return sectionRepository
                .findSection(bookId, sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId, bookId));
    }

    private static String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("X-Username must not be blank.");
        }

        return username.strip();
    }
}
