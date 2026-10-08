package fpenim.adventurebookapi.book.integration;

import com.jayway.jsonpath.JsonPath;
import fpenim.adventurebookapi.book.BookImportService;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookControllerTest {

    private static final String CAVERNS = "The Crystal Caverns";
    private static final String TOWER = "The Sunken Tower";
    private static final long UNKNOWN_BOOK_ID = 999_999L;
    private static final int MAX_STEPS = 50;
    private static final String BOOK_PATH = "/books/{id}";
    private static final String SECTION_PATH = "/books/{id}/sections/{sectionId}";

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
    private long towerId;

    @BeforeEach
    void importBooks() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE books RESTART IDENTITY CASCADE");

        try (InputStream input = new ClassPathResource("seed/books/valid-crystal-caverns.json").getInputStream()) {
            bookImportService.importBook(jsonMapper.readValue(input, BookImportRequest.class));
        }

        // Begins on section 5 so its begin link differs from the caverns book, which begins on section 1.
        bookImportService.importBook(new BookImportRequest(
                TOWER,
                "Mara Voss",
                List.of("mystery"),
                Difficulty.HARD,
                List.of(
                        new Section(
                                5,
                                "You wake at the foot of a drowned tower.",
                                SectionType.BEGIN,
                                List.of(new Option("Climb the stairs", 6, List.of()))),
                        new Section(6, "The tower top opens to the sky.", SectionType.END, List.of()))));

        cavernsId = bookId(CAVERNS);
        towerId = bookId(TOWER);
    }

    // Books

    @Test
    void getBooksReturnsEachBookWithItsOwnSelfAndBeginLinks() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(2))
                .andExpect(jsonPath(embeddedBook(CAVERNS) + "._links.self.href")
                        .value(contains(endsWith("/books/" + cavernsId))))
                .andExpect(jsonPath(embeddedBook(CAVERNS) + "._links.begin.href")
                        .value(contains(endsWith("/books/" + cavernsId + "/sections/1"))))
                .andExpect(jsonPath(embeddedBook(TOWER) + "._links.self.href")
                        .value(contains(endsWith("/books/" + towerId))))
                .andExpect(jsonPath(embeddedBook(TOWER) + "._links.begin.href")
                        .value(contains(endsWith("/books/" + towerId + "/sections/5"))));
    }

    @Test
    void getBooksKeepsTheBeginLinkWhenFiltered() throws Exception {
        mockMvc.perform(get("/books").param("title", "tower"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(1))
                .andExpect(jsonPath("$._embedded.books[0].title").value(TOWER))
                .andExpect(jsonPath("$._embedded.books[0]._links.begin.href")
                        .value(endsWith("/books/" + towerId + "/sections/5")));
    }

    @Test
    void getBooksReturns404WhenAMatchingBookHasNoBeginSection() throws Exception {
        long brokenId = insertBookWithoutSections();

        mockMvc.perform(get("/books"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("BEGIN section on book id [" + brokenId + "] not found."));
    }

    // Book

    @Test
    void getBookReturnsBookWithBeginLink() throws Exception {
        mockMvc.perform(get(BOOK_PATH, cavernsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(CAVERNS))
                .andExpect(jsonPath("$.author").value("Evelyn Stormrider"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$._links.begin.href").value(endsWith("/books/" + cavernsId + "/sections/1")));
    }

    @Test
    void getBookReturns404ForUnknownBook() throws Exception {
        mockMvc.perform(get(BOOK_PATH, UNKNOWN_BOOK_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_BOOK_ID))));
    }

    @Test
    void getBookReturns404WhenBookHasNoBeginSection() throws Exception {
        long brokenId = insertBookWithoutSections();

        mockMvc.perform(get(BOOK_PATH, brokenId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("BEGIN section on book id [" + brokenId + "] not found."));
    }

    // Section

    @Test
    void getSectionReturnsTextTypeAndOptionsInStoredOrder() throws Exception {
        mockMvc.perform(get(SECTION_PATH, cavernsId, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("BEGIN"))
                .andExpect(jsonPath("$.text").value(startsWith("You stand at the entrance")))
                .andExpect(jsonPath("$.options.length()").value(2))
                .andExpect(jsonPath("$.options[0].description").value("Cross the rope bridge carefully"))
                .andExpect(jsonPath("$.options[1].description").value("Search the rocky walls for another path"));
    }

    @Test
    void getSectionLinksEachOptionToItsTargetAndHidesGotoId() throws Exception {
        mockMvc.perform(get(SECTION_PATH, cavernsId, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0]._links.goTo.href")
                        .value(endsWith("/books/" + cavernsId + "/sections/100")))
                .andExpect(jsonPath("$.options[1]._links.goTo.href")
                        .value(endsWith("/books/" + cavernsId + "/sections/20")))
                .andExpect(jsonPath("$.options[0].gotoId").doesNotExist())
                .andExpect(jsonPath("$.options[1].gotoId").doesNotExist());
    }

    @Test
    void getSectionIncludesConsequencesOnlyWhenAnOptionHasThem() throws Exception {
        mockMvc.perform(get(SECTION_PATH, cavernsId, 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].consequences.length()").value(1))
                .andExpect(jsonPath("$.options[0].consequences[0].type").value("LOSE_HEALTH"))
                .andExpect(jsonPath("$.options[0].consequences[0].value").value(4))
                .andExpect(jsonPath("$.options[0].consequences[0].text")
                        .value("You scrape your shoulder squeezing through the narrow gap."))
                .andExpect(jsonPath("$.options[1].consequences").doesNotExist());
    }

    @Test
    void getSectionReturnsEmptyOptionsForAnEndSection() throws Exception {
        mockMvc.perform(get(SECTION_PATH, cavernsId, 800))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("END"))
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").isEmpty());
    }

    @Test
    void getSectionReturns404ForUnknownSection() throws Exception {
        mockMvc.perform(get(SECTION_PATH, cavernsId, 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("999")));
    }

    @Test
    void getSectionReturns404ForSectionThatOnlyExistsInAnotherBook() throws Exception {
        mockMvc.perform(get(SECTION_PATH, towerId, 20)).andExpect(status().isNotFound());
    }

    @Test
    void getSectionReturns404ForUnknownBook() throws Exception {
        mockMvc.perform(get(SECTION_PATH, UNKNOWN_BOOK_ID, 1)).andExpect(status().isNotFound());
    }

    @Test
    void getSectionReturns400ForNonNumericSectionId() throws Exception {
        mockMvc.perform(get(SECTION_PATH, cavernsId, "abc")).andExpect(status().isBadRequest());
    }

    // Reading a whole book

    @Test
    void followingLinksFromTheBookReachesAnEndSection() throws Exception {
        String href = JsonPath.read(body(get(BOOK_PATH, cavernsId)), "$._links.begin.href");

        for (int step = 0; step < MAX_STEPS; step++) {
            String section = body(get(URI.create(href)));
            List<String> nextLinks = JsonPath.read(section, "$.options[*]._links.goTo.href");

            if (nextLinks.isEmpty()) {
                assertThat(JsonPath.<String>read(section, "$.type")).isEqualTo("END");
                return;
            }

            href = nextLinks.getFirst();
        }

        fail("No END section reached after " + MAX_STEPS + " steps");
    }

    private String body(RequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String embeddedBook(String title) {
        return "$._embedded.books[?(@.title == '" + title + "')]";
    }

    private long bookId(String title) {
        return jdbcTemplate.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }

    private long insertBookWithoutSections() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO books (title, author, difficulty) VALUES ('Broken Book', 'Nobody', 'EASY') RETURNING id",
                Long.class);
    }
}
