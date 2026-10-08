package fpenim.adventurebookapi.adventure.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fpenim.adventurebookapi.book.BookImportService;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.model.section.option.ConsequenceType;
import fpenim.adventurebookapi.book.model.section.option.Option;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
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
    private static final String BRIDGE = "The Goblin Bridge";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final long UNKNOWN_ID = 999_999L;
    private static final String START_PATH = "/books/{bookId}/adventure/start";
    private static final String ADVENTURES_PATH = "/adventures";
    private static final String ADVENTURE_PATH = "/adventures/{id}";
    private static final String MOVE_PATH = "/adventures/{id}/move";

    // Options of the bridge book, by section.
    private static final int FIGHT = 0; // section 1 -> 2, lose 4
    private static final int SNEAK = 1; // section 1 -> 3
    private static final int JUMP = 2; // section 1 -> 2, lose 15
    private static final int DRINK = 0; // section 2 -> 3, gain 3
    private static final int FIGHT_AGAIN = 1; // section 2 -> 1, lose 4
    private static final int OPEN_DOOR = 0; // section 3 -> 4
    private static final int TRAP_CORRIDOR = 1; // section 3 -> 4, lose 2 then gain 1

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
    private long bridgeId;

    @BeforeEach
    void importBooks() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE books RESTART IDENTITY CASCADE");

        try (InputStream input = new ClassPathResource("seed/books/valid-crystal-caverns.json").getInputStream()) {
            bookImportService.importBook(jsonMapper.readValue(input, BookImportRequest.class));
        }

        importBridgeBook();

        cavernsId = bookId(CAVERNS);
        bridgeId = bookId(BRIDGE);
    }

    // Start

    @Test
    void startAdventureCreatesAnAdventureAtTheBeginSectionWithFullHealth() throws Exception {
        mockMvc.perform(post(START_PATH, bridgeId).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(ALICE))
                .andExpect(jsonPath("$.bookId").value(bridgeId))
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.section.id").value(1))
                .andExpect(jsonPath("$.section.type").value("BEGIN"));
    }

    @Test
    void startAdventureReturnsTheLocationOfTheNewAdventure() throws Exception {
        String location = mockMvc.perform(post(START_PATH, bridgeId).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/adventures/")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(URI.create(location)).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(bridgeId));
    }

    @Test
    void startAdventureCanBeReachedThroughTheStartLinkOfABook() throws Exception {
        String book = mockMvc.perform(get("/books/{id}", bridgeId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String href = JsonPath.read(book, "$._links.start.href");

        mockMvc.perform(post(URI.create(href)).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(bridgeId));
    }

    @Test
    void startAdventureTwiceOnTheSameBookGivesTwoIndependentAdventures() throws Exception {
        long first = start(ALICE, bridgeId);
        long second = start(ALICE, bridgeId);

        move(ALICE, first, FIGHT).andExpect(status().isOk());

        mockMvc.perform(get(ADVENTURE_PATH, first).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$.health").value(6))
                .andExpect(jsonPath("$.section.id").value(2));
        mockMvc.perform(get(ADVENTURE_PATH, second).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.section.id").value(1));
    }

    @Test
    void startAdventureStripsWhitespaceAroundTheUsername() throws Exception {
        mockMvc.perform(post(START_PATH, bridgeId).header(USERNAME_HEADER, "  " + ALICE + " "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(ALICE));
    }

    @Test
    void startAdventureReturns404ForUnknownBook() throws Exception {
        mockMvc.perform(post(START_PATH, UNKNOWN_ID).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_ID))));
    }

    @Test
    void startAdventureReturns400WithoutUsername() throws Exception {
        mockMvc.perform(post(START_PATH, bridgeId)).andExpect(status().isBadRequest());
    }

    @Test
    void startAdventureReturns400ForBlankUsername() throws Exception {
        mockMvc.perform(post(START_PATH, bridgeId).header(USERNAME_HEADER, " ")).andExpect(status().isBadRequest());
    }

    // Get

    @Test
    void getAdventureReturnsTheCurrentSectionWithNumberedOptions() throws Exception {
        long id = start(ALICE, bridgeId);

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.section.text").value("A goblin guards the bridge."))
                .andExpect(jsonPath("$.section.options.length()").value(3))
                .andExpect(jsonPath("$.section.options[0].option").value(0))
                .andExpect(jsonPath("$.section.options[0].description").value("Fight the goblin"))
                .andExpect(jsonPath("$.section.options[0].consequences[0].type").value("LOSE_HEALTH"))
                .andExpect(
                        jsonPath("$.section.options[0].consequences[0].value").value(4))
                .andExpect(jsonPath("$.section.options[1].option").value(1))
                .andExpect(jsonPath("$.section.options[1].consequences").doesNotExist())
                .andExpect(jsonPath("$.section.options[2].option").value(2));
    }

    @Test
    void getAdventureLinksToItselfItsBookAndTheMove() throws Exception {
        long id = start(ALICE, bridgeId);

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/adventures/" + id)))
                .andExpect(jsonPath("$._links.book.href").value(endsWith("/books/" + bridgeId)))
                .andExpect(jsonPath("$._links.move.href").value(endsWith("/adventures/" + id + "/move")));
    }

    @Test
    void getAdventureReturns404ForUnknownAdventure() throws Exception {
        mockMvc.perform(get(ADVENTURE_PATH, UNKNOWN_ID).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_ID))));
    }

    @Test
    void getAdventureReturns404ForAnotherUsersAdventure() throws Exception {
        long id = start(ALICE, bridgeId);

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, BOB)).andExpect(status().isNotFound());
    }

    @Test
    void getAdventureReturns400WithoutUsername() throws Exception {
        long id = start(ALICE, bridgeId);

        mockMvc.perform(get(ADVENTURE_PATH, id)).andExpect(status().isBadRequest());
    }

    // Move

    @Test
    void moveGoesToTheSectionOfTheChosenOption() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, SNEAK)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section.id").value(3))
                .andExpect(jsonPath("$.health").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$.section.id").value(3));
    }

    @Test
    void moveAppliesLoseAndGainHealthConsequences() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, FIGHT).andExpect(jsonPath("$.health").value(6));
        move(ALICE, id, DRINK)
                .andExpect(jsonPath("$.health").value(9))
                .andExpect(jsonPath("$.section.id").value(3));
    }

    @Test
    void moveAppliesEveryConsequenceOfTheOptionInOrder() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, SNEAK);
        move(ALICE, id, TRAP_CORRIDOR).andExpect(jsonPath("$.health").value(9));
    }

    @Test
    void moveCanRaiseHealthAboveTheStartingValue() throws Exception {
        long id = start(ALICE, bridgeId);
        jdbcTemplate.update("UPDATE adventures SET current_section_id = 2 WHERE id = ?", id);

        move(ALICE, id, DRINK).andExpect(jsonPath("$.health").value(13));
    }

    @Test
    void moveKillsThePlayerWhenHealthReachesZero() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, JUMP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(0))
                .andExpect(jsonPath("$.status").value("DEAD"))
                .andExpect(jsonPath("$._links.move").doesNotExist());
    }

    @Test
    void moveKillsThePlayerAfterSeveralLosses() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, FIGHT).andExpect(jsonPath("$.health").value(6));
        move(ALICE, id, FIGHT_AGAIN).andExpect(jsonPath("$.health").value(2));
        move(ALICE, id, FIGHT)
                .andExpect(jsonPath("$.health").value(0))
                .andExpect(jsonPath("$.status").value("DEAD"));
    }

    @Test
    void moveReturns409WhenThePlayerIsDead() throws Exception {
        long id = start(ALICE, bridgeId);
        move(ALICE, id, JUMP);

        move(ALICE, id, DRINK)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("DEAD")));

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$.health").value(0))
                .andExpect(jsonPath("$.section.id").value(2));
    }

    @Test
    void moveFinishesTheAdventureOnAnEndSection() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, SNEAK);
        move(ALICE, id, OPEN_DOOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.section.type").value("END"))
                .andExpect(jsonPath("$.section.options").isEmpty())
                .andExpect(jsonPath("$._links.move").doesNotExist());
    }

    @Test
    void moveReturns409WhenTheAdventureIsFinished() throws Exception {
        long id = start(ALICE, bridgeId);
        move(ALICE, id, SNEAK);
        move(ALICE, id, OPEN_DOOR);

        move(ALICE, id, 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("FINISHED")));
    }

    @Test
    void moveReturns400ForAnOptionTheCurrentSectionDoesNotHave() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, 3)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("3")));

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$.section.id").value(1))
                .andExpect(jsonPath("$.health").value(10));
    }

    @Test
    void moveReturns400ForNegativeOption() throws Exception {
        long id = start(ALICE, bridgeId);

        move(ALICE, id, -1).andExpect(status().isBadRequest());
    }

    @Test
    void moveReturns400WithoutOption() throws Exception {
        long id = start(ALICE, bridgeId);

        mockMvc.perform(post(MOVE_PATH, id)
                        .header(USERNAME_HEADER, ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moveReturns404ForUnknownAdventure() throws Exception {
        move(ALICE, UNKNOWN_ID, 0).andExpect(status().isNotFound());
    }

    @Test
    void moveReturns404ForAnotherUsersAdventureAndLeavesItUntouched() throws Exception {
        long id = start(ALICE, bridgeId);

        move(BOB, id, FIGHT).andExpect(status().isNotFound());

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$.section.id").value(1))
                .andExpect(jsonPath("$.health").value(10));
    }

    // List

    @Test
    void getAdventuresReturnsTheUsersAdventuresAcrossBooks() throws Exception {
        long bridge = start(ALICE, bridgeId);
        long caverns = start(ALICE, cavernsId);
        move(ALICE, bridge, FIGHT);

        mockMvc.perform(get(ADVENTURES_PATH).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.adventures.length()").value(2))
                .andExpect(jsonPath("$._embedded.adventures[0].id").value(bridge))
                .andExpect(jsonPath("$._embedded.adventures[0].bookId").value(bridgeId))
                .andExpect(jsonPath("$._embedded.adventures[0].bookTitle").value(BRIDGE))
                .andExpect(
                        jsonPath("$._embedded.adventures[0].currentSectionId").value(2))
                .andExpect(jsonPath("$._embedded.adventures[0].health").value(6))
                .andExpect(jsonPath("$._embedded.adventures[0].status").value("IN_PROGRESS"))
                .andExpect(
                        jsonPath("$._embedded.adventures[0]._links.self.href").value(endsWith("/adventures/" + bridge)))
                .andExpect(jsonPath("$._embedded.adventures[1].id").value(caverns))
                .andExpect(jsonPath("$._embedded.adventures[1].bookTitle").value(CAVERNS));
    }

    @Test
    void getAdventuresShowsDeadAndFinishedAdventures() throws Exception {
        long dead = start(ALICE, bridgeId);
        long finished = start(ALICE, bridgeId);
        move(ALICE, dead, JUMP);
        move(ALICE, finished, SNEAK);
        move(ALICE, finished, OPEN_DOOR);

        mockMvc.perform(get(ADVENTURES_PATH).header(USERNAME_HEADER, ALICE))
                .andExpect(jsonPath("$._embedded.adventures[*].status", containsInAnyOrder("DEAD", "FINISHED")));
    }

    @Test
    void getAdventuresReturnsOnlyTheCallersAdventures() throws Exception {
        start(ALICE, bridgeId);
        long bobs = start(BOB, bridgeId);

        mockMvc.perform(get(ADVENTURES_PATH).header(USERNAME_HEADER, BOB))
                .andExpect(jsonPath("$._embedded.adventures.length()").value(1))
                .andExpect(jsonPath("$._embedded.adventures[0].id").value(bobs));
    }

    @Test
    void getAdventuresIsEmptyForAUserWithoutAdventures() throws Exception {
        start(ALICE, bridgeId);

        mockMvc.perform(get(ADVENTURES_PATH).header(USERNAME_HEADER, BOB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.adventures").doesNotExist());
    }

    @Test
    void getAdventuresReturns400WithoutUsername() throws Exception {
        mockMvc.perform(get(ADVENTURES_PATH)).andExpect(status().isBadRequest());
    }

    // Delete

    @Test
    void deleteAdventureRemovesOnlyThatAdventure() throws Exception {
        long deleted = start(ALICE, bridgeId);
        long kept = start(ALICE, bridgeId);

        mockMvc.perform(delete(ADVENTURE_PATH, deleted).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ADVENTURE_PATH, deleted).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ADVENTURE_PATH, kept).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAdventureReturns404ForUnknownAdventure() throws Exception {
        mockMvc.perform(delete(ADVENTURE_PATH, UNKNOWN_ID).header(USERNAME_HEADER, ALICE))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAdventureReturns404ForAnotherUsersAdventureAndKeepsIt() throws Exception {
        long id = start(ALICE, bridgeId);

        mockMvc.perform(delete(ADVENTURE_PATH, id).header(USERNAME_HEADER, BOB)).andExpect(status().isNotFound());

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE)).andExpect(status().isOk());
    }

    @Test
    void deletingABookRemovesItsAdventures() throws Exception {
        long id = start(ALICE, bridgeId);

        jdbcTemplate.update("DELETE FROM books WHERE id = ?", bridgeId);

        mockMvc.perform(get(ADVENTURE_PATH, id).header(USERNAME_HEADER, ALICE)).andExpect(status().isNotFound());
    }

    private void importBridgeBook() {
        bookImportService.importBook(new BookImportRequest(
                BRIDGE,
                "Tobias Ferrow",
                List.of(),
                Difficulty.MEDIUM,
                List.of(
                        new Section(
                                1,
                                "A goblin guards the bridge.",
                                SectionType.BEGIN,
                                List.of(
                                        new Option("Fight the goblin", 2, List.of(lose(4))),
                                        new Option("Sneak past", 3, List.of()),
                                        new Option("Jump into the chasm", 2, List.of(lose(15))))),
                        new Section(
                                2,
                                "You lie bruised under the bridge.",
                                SectionType.NODE,
                                List.of(
                                        new Option("Drink the potion", 3, List.of(gain(3))),
                                        new Option("Climb back and fight again", 1, List.of(lose(4))))),
                        new Section(
                                3,
                                "A door stands at the far side.",
                                SectionType.NODE,
                                List.of(
                                        new Option("Open the door", 4, List.of()),
                                        new Option("Take the trapped corridor", 4, List.of(lose(2), gain(1))))),
                        new Section(4, "You are across.", SectionType.END, List.of()))));
    }

    private static Consequence lose(int value) {
        return new Consequence(ConsequenceType.LOSE_HEALTH, value, null);
    }

    private static Consequence gain(int value) {
        return new Consequence(ConsequenceType.GAIN_HEALTH, value, null);
    }

    private long start(String username, long bookId) throws Exception {
        String body = mockMvc.perform(post(START_PATH, bookId).header(USERNAME_HEADER, username))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private ResultActions move(String username, long id, int option) throws Exception {
        return mockMvc.perform(post(MOVE_PATH, id)
                .header(USERNAME_HEADER, username)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\": " + option + "}"));
    }

    private long bookId(String title) {
        return jdbcTemplate.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }
}
