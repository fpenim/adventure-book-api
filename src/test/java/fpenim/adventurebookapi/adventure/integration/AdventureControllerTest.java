package fpenim.adventurebookapi.adventure.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fpenim.adventurebookapi.book.BookImportService;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.model.section.option.ConsequenceType;
import fpenim.adventurebookapi.book.model.section.option.Option;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdventureControllerTest {

    private static final String CAVERNS = "The Crystal Caverns";
    private static final String PIT = "The Spiked Pit";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final long UNKNOWN_BOOK_ID = 999_999L;
    private static final String ADVENTURE_PATH = "/adventures/{bookId}";
    private static final String CHOICE_PATH = "/adventures/{bookId}/sections/{sectionId}/options/{position}";
    private static final String RESTART_PATH = "/adventures/{bookId}/restart";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private BookImportService bookImportService;

    private long cavernsId;
    private long pitId;

    @BeforeEach
    void importBooks() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE books RESTART IDENTITY CASCADE");

        try (InputStream input = new ClassPathResource("seed/books/valid-crystal-caverns.json").getInputStream()) {
            bookImportService.importBook(jsonMapper.readValue(input, BookImportRequest.class));
        }
        importPit();

        cavernsId = bookId(CAVERNS);
        pitId = bookId(PIT);
    }

    // Start

    @Test
    void startPutsThePlayerOnTheBeginSectionWithFullHealth() throws Exception {
        start(ALICE, cavernsId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(ALICE))
                .andExpect(jsonPath("$.bookId").value(cavernsId))
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.section.id").value(1))
                .andExpect(jsonPath("$.section.type").value("BEGIN"));
    }

    @Test
    void startLinksToItselfRestartAndEachChoice() throws Exception {
        String adventure = "/adventures/" + cavernsId;

        start(ALICE, cavernsId)
                .andExpect(jsonPath("$._links.self.href").value(endsWith(adventure)))
                .andExpect(jsonPath("$._links.restart.href").value(endsWith(adventure + "/restart")))
                .andExpect(jsonPath("$.section.options[0]._links.choose.href")
                        .value(endsWith(adventure + "/sections/1/options/0")))
                .andExpect(jsonPath("$.section.options[1]._links.choose.href")
                        .value(endsWith(adventure + "/sections/1/options/1")));
    }

    @Test
    void startReturnsExistingProgressUnchangedWhenAlreadyStarted() throws Exception {
        start(ALICE, cavernsId);
        choose(ALICE, cavernsId, 1, 1);
        choose(ALICE, cavernsId, 20, 0);

        start(ALICE, cavernsId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(6))
                .andExpect(jsonPath("$.section.id").value(200));
    }

    @Test
    void startReturns404ForUnknownBook() throws Exception {
        start(ALICE, UNKNOWN_BOOK_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_BOOK_ID))));
    }

    @Test
    void requestsReturn400WithoutUsernameHeader() throws Exception {
        mockMvc.perform(post(ADVENTURE_PATH, cavernsId)).andExpect(status().isBadRequest());
        mockMvc.perform(get(ADVENTURE_PATH, cavernsId)).andExpect(status().isBadRequest());
        mockMvc.perform(post(CHOICE_PATH, cavernsId, 1, 0)).andExpect(status().isBadRequest());
        mockMvc.perform(post(RESTART_PATH, cavernsId)).andExpect(status().isBadRequest());
    }

    @Test
    void requestsReturn400ForBlankUsername() throws Exception {
        start(" ", cavernsId).andExpect(status().isBadRequest());
        current(" ", cavernsId).andExpect(status().isBadRequest());
    }

    // Current

    @Test
    void currentReturnsWhereThePlayerLeftOff() throws Exception {
        start(ALICE, cavernsId);
        choose(ALICE, cavernsId, 1, 1);
        choose(ALICE, cavernsId, 20, 0);

        current(ALICE, cavernsId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(6))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.section.id").value(200))
                .andExpect(jsonPath("$.section.options[0]._links.choose.href")
                        .value(endsWith("/adventures/" + cavernsId + "/sections/200/options/0")));
    }

    @Test
    void currentReturns404ForBookThePlayerHasNotStarted() throws Exception {
        start(ALICE, cavernsId);

        current(ALICE, pitId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(ALICE)));
    }

    // Choose

    @Test
    void chooseMovesWithoutChangingHealthWhenTheOptionHasNoConsequence() throws Exception {
        start(ALICE, cavernsId);

        choose(ALICE, cavernsId, 1, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.section.id").value(100));
    }

    @Test
    void chooseAppliesLoseAndGainHealthConsequences() throws Exception {
        start(ALICE, cavernsId);
        choose(ALICE, cavernsId, 1, 0);

        choose(ALICE, cavernsId, 100, 0).andExpect(jsonPath("$.section.id").value(300));
        choose(ALICE, cavernsId, 300, 1)
                .andExpect(jsonPath("$.health").value(13))
                .andExpect(jsonPath("$.section.id").value(700));
        choose(ALICE, cavernsId, 700, 0);
        choose(ALICE, cavernsId, 600, 1)
                .andExpect(jsonPath("$.health").value(8))
                .andExpect(jsonPath("$.section.id").value(1000));
    }

    @Test
    void chooseAppliesEveryConsequenceOfAnOption() throws Exception {
        start(ALICE, pitId);

        choose(ALICE, pitId, 1, 1)
                .andExpect(jsonPath("$.health").value(9))
                .andExpect(jsonPath("$.section.id").value(2));
    }

    @Test
    void chooseKillsThePlayerWhenHealthRunsOutAndBlocksFurtherProgress() throws Exception {
        start(ALICE, pitId);

        choose(ALICE, pitId, 1, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(0))
                .andExpect(jsonPath("$.status").value("DEAD"))
                .andExpect(jsonPath("$.section.id").value(2))
                .andExpect(jsonPath("$.section.options[0]._links").doesNotExist())
                .andExpect(jsonPath("$._links.restart.href").exists());

        choose(ALICE, pitId, 2, 0).andExpect(status().isConflict());
        current(ALICE, pitId)
                .andExpect(jsonPath("$.status").value("DEAD"))
                .andExpect(jsonPath("$.section.id").value(2));
    }

    @Test
    void chooseFinishesTheBookOnAnEndSectionAndBlocksFurtherProgress() throws Exception {
        start(ALICE, pitId);
        choose(ALICE, pitId, 1, 1);

        choose(ALICE, pitId, 2, 0)
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.section.type").value("END"));

        choose(ALICE, pitId, 3, 0).andExpect(status().isConflict());
    }

    @Test
    void chooseReturns409ForASectionThePlayerIsNotOn() throws Exception {
        start(ALICE, cavernsId);
        choose(ALICE, cavernsId, 1, 1);

        // Repeating the request must not move the player a second time.
        choose(ALICE, cavernsId, 1, 1).andExpect(status().isConflict());
        choose(ALICE, cavernsId, 600, 1).andExpect(status().isConflict());

        current(ALICE, cavernsId)
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.section.id").value(20));
    }

    @Test
    void chooseReturns404ForUnknownOption() throws Exception {
        start(ALICE, cavernsId);

        choose(ALICE, cavernsId, 1, 2).andExpect(status().isNotFound());
        choose(ALICE, cavernsId, 1, -1).andExpect(status().isNotFound());
    }

    @Test
    void chooseReturns404ForBookThePlayerHasNotStarted() throws Exception {
        choose(ALICE, cavernsId, 1, 0).andExpect(status().isNotFound());
    }

    // Restart

    @Test
    void restartPutsADeadPlayerBackOnTheBeginSectionWithFullHealth() throws Exception {
        start(ALICE, pitId);
        choose(ALICE, pitId, 1, 0).andExpect(jsonPath("$.status").value("DEAD"));

        restart(ALICE, pitId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.section.id").value(1))
                .andExpect(jsonPath("$.section.options[0]._links.choose.href").exists());

        choose(ALICE, pitId, 1, 1).andExpect(status().isOk());
    }

    @Test
    void restartWorksMidBookAndAfterFinishing() throws Exception {
        start(ALICE, pitId);
        choose(ALICE, pitId, 1, 1);

        restart(ALICE, pitId)
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.section.id").value(1));

        choose(ALICE, pitId, 1, 1);
        choose(ALICE, pitId, 2, 0).andExpect(jsonPath("$.status").value("FINISHED"));

        restart(ALICE, pitId)
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.section.id").value(1));
    }

    @Test
    void restartReturns404ForBookThePlayerHasNotStarted() throws Exception {
        restart(ALICE, cavernsId).andExpect(status().isNotFound());
        restart(ALICE, UNKNOWN_BOOK_ID).andExpect(status().isNotFound());
    }

    // Several books and several players

    @Test
    void onePlayerProgressesIndependentlyInEachBook() throws Exception {
        start(ALICE, cavernsId);
        start(ALICE, pitId);

        choose(ALICE, pitId, 1, 0).andExpect(jsonPath("$.status").value("DEAD"));
        choose(ALICE, cavernsId, 1, 0).andExpect(status().isOk());

        current(ALICE, cavernsId)
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.section.id").value(100));

        restart(ALICE, pitId);
        current(ALICE, cavernsId).andExpect(jsonPath("$.section.id").value(100));
    }

    @Test
    void playersReadingTheSameBookProgressIndependently() throws Exception {
        start(ALICE, cavernsId);
        start(BOB, cavernsId);

        choose(ALICE, cavernsId, 1, 1);
        choose(ALICE, cavernsId, 20, 0);

        current(ALICE, cavernsId)
                .andExpect(jsonPath("$.health").value(6))
                .andExpect(jsonPath("$.section.id").value(200));
        current(BOB, cavernsId)
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.section.id").value(1));
    }

    private ResultActions start(String username, long bookId) throws Exception {
        return mockMvc.perform(post(ADVENTURE_PATH, bookId).header(USERNAME_HEADER, username));
    }

    private ResultActions current(String username, long bookId) throws Exception {
        return mockMvc.perform(get(ADVENTURE_PATH, bookId).header(USERNAME_HEADER, username));
    }

    private ResultActions choose(String username, long bookId, int sectionId, int position) throws Exception {
        return mockMvc.perform(post(CHOICE_PATH, bookId, sectionId, position).header(USERNAME_HEADER, username));
    }

    private ResultActions restart(String username, long bookId) throws Exception {
        return mockMvc.perform(post(RESTART_PATH, bookId).header(USERNAME_HEADER, username));
    }

    // Unlike the caverns book, a player can die here on a section that still has options.
    private void importPit() {
        bookImportService.importBook(new BookImportRequest(
                PIT,
                "Mara Voss",
                List.of(),
                Difficulty.HARD,
                List.of(
                        new Section(
                                1,
                                "A pit of spikes blocks the corridor.",
                                SectionType.BEGIN,
                                List.of(
                                        new Option(
                                                "Jump in",
                                                2,
                                                List.of(new Consequence(ConsequenceType.LOSE_HEALTH, 15, null))),
                                        new Option(
                                                "Climb around",
                                                2,
                                                List.of(
                                                        new Consequence(ConsequenceType.LOSE_HEALTH, 3, null),
                                                        new Consequence(ConsequenceType.GAIN_HEALTH, 2, null))))),
                        new Section(
                                2,
                                "You are on the far side of the pit.",
                                SectionType.NODE,
                                List.of(new Option("Walk on", 3, List.of()))),
                        new Section(3, "Daylight.", SectionType.END, List.of()))));
    }

    private long bookId(String title) {
        return jdbcTemplate.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }
}
