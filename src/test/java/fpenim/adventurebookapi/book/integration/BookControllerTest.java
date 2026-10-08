package fpenim.adventurebookapi.book.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fpenim.adventurebookapi.book.BookImportService;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookControllerTest {

    private static final String CAVERNS = "The Crystal Caverns";
    private static final String TOWER = "The Sunken Tower";
    private static final String SPECIAL = "100% Pure_Gold!";
    private static final String DESERT = "The Glass Desert";
    private static final long UNKNOWN_BOOK_ID = 999_999L;
    private static final int MAX_STEPS = 50;
    private static final String BOOK_PATH = "/books/{id}";
    private static final String CATEGORY_PATH = "/books/{id}/categories/{category}";
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

        importBook(TOWER, "Mara Voss", List.of("mystery"), Difficulty.HARD);

        cavernsId = bookId(CAVERNS);
        towerId = bookId(TOWER);
    }

    // Books

    @Test
    void getBooksReturnsEachBookWithItsOwnSelfBeginAndStartLinks() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(2))
                .andExpect(jsonPath(embeddedBook(CAVERNS) + "._links.self.href")
                        .value(contains(endsWith("/books/" + cavernsId))))
                .andExpect(jsonPath(embeddedBook(CAVERNS) + "._links.begin.href")
                        .value(contains(endsWith("/books/" + cavernsId + "/sections/1"))))
                .andExpect(jsonPath(embeddedBook(CAVERNS) + "._links.start.href")
                        .value(contains(endsWith("/adventures/start"))))
                .andExpect(jsonPath(embeddedBook(TOWER) + "._links.self.href")
                        .value(contains(endsWith("/books/" + towerId))))
                .andExpect(jsonPath(embeddedBook(TOWER) + "._links.begin.href")
                        .value(contains(endsWith("/books/" + towerId + "/sections/5"))))
                .andExpect(jsonPath(embeddedBook(TOWER) + "._links.start.href")
                        .value(contains(endsWith("/adventures/start"))));
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

    // Books filters

    @Test
    void getBooksFiltersByAuthorIgnoringCaseAndSurroundingWhitespace() throws Exception {
        mockMvc.perform(get("/books").param("author", "  STORM "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(1))
                .andExpect(jsonPath("$._embedded.books[0].title").value(CAVERNS));
    }

    @Test
    void getBooksFiltersByCategoryIgnoringCase() throws Exception {
        mockMvc.perform(get("/books").param("category", " Mystery "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(1))
                .andExpect(jsonPath("$._embedded.books[0].title").value(TOWER));
    }

    @Test
    void getBooksMatchesCategoryExactlyRatherThanPartially() throws Exception {
        mockMvc.perform(get("/books").param("category", "myst"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books").doesNotExist());
    }

    @Test
    void getBooksFiltersByDifficulty() throws Exception {
        mockMvc.perform(get("/books").param("difficulty", "HARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(1))
                .andExpect(jsonPath("$._embedded.books[0].title").value(TOWER));
    }

    @Test
    void getBooksReturns400ForUnknownDifficulty() throws Exception {
        mockMvc.perform(get("/books").param("difficulty", "IMPOSSIBLE")).andExpect(status().isBadRequest());
    }

    @Test
    void getBooksCombinesFiltersWithAnd() throws Exception {
        mockMvc.perform(get("/books").param("author", "voss").param("difficulty", "HARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(1))
                .andExpect(jsonPath("$._embedded.books[0].title").value(TOWER));

        mockMvc.perform(get("/books").param("author", "voss").param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books").doesNotExist());
    }

    @Test
    void getBooksIgnoresBlankFilters() throws Exception {
        mockMvc.perform(get("/books").param("title", " ").param("author", "").param("category", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books.length()").value(2));
    }

    @Test
    void getBooksTreatsLikeWildcardsInTitleAndAuthorAsLiterals() throws Exception {
        importBook(SPECIAL, "A_B 50% !", List.of(), Difficulty.MEDIUM);

        for (String filter : List.of("title", "author")) {
            for (String special : List.of("%", "_", "!")) {
                mockMvc.perform(get("/books").param(filter, special))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.books.length()").value(1))
                        .andExpect(jsonPath("$._embedded.books[0].title").value(SPECIAL));
            }
        }
    }

    // Book

    @Test
    void getBookReturnsBookWithBeginAndStartLinks() throws Exception {
        mockMvc.perform(get(BOOK_PATH, cavernsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(CAVERNS))
                .andExpect(jsonPath("$.author").value("Evelyn Stormrider"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$._links.begin.href").value(endsWith("/books/" + cavernsId + "/sections/1")))
                .andExpect(jsonPath("$._links.start.href").value(endsWith("/adventures/start")));
    }

    @Test
    void getBookReturns404ForUnknownBook() throws Exception {
        mockMvc.perform(get(BOOK_PATH, UNKNOWN_BOOK_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_BOOK_ID))));
    }

    // Add book

    @Test
    void addBookReturns201WithLocationAndTheNewBook() throws Exception {
        String location = mockMvc.perform(addRequest(towerBook(DESERT, "Idris Vale", List.of("survival"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(DESERT))
                .andExpect(jsonPath("$.author").value("Idris Vale"))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.categories", contains("survival")))
                .andExpect(jsonPath("$._links.start.href").value(endsWith("/adventures/start")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        long desertId = bookId(DESERT);

        assertThat(location).endsWith("/books/" + desertId);
        mockMvc.perform(get(URI.create(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(desertId))
                .andExpect(jsonPath("$._links.begin.href").value(endsWith("/books/" + desertId + "/sections/5")));
        mockMvc.perform(get("/books"))
                .andExpect(jsonPath("$._embedded.books.length()").value(3));
    }

    @Test
    void addBookAcceptsTheSeedFileFormat() throws Exception {
        try (InputStream input = new ClassPathResource("seed/books/valid-crystal-caverns.json").getInputStream()) {
            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(input.readAllBytes()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value(CAVERNS))
                    .andExpect(jsonPath("$.beginSectionId").value(1));
        }
    }

    @Test
    void addBookReturns400ForBlankTitle() throws Exception {
        mockMvc.perform(addRequest(towerBook(" ", "Idris Vale", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("Invalid book")))
                .andExpect(jsonPath("$.detail").value(containsString("title")));

        assertNoBookWasAdded();
    }

    @Test
    void addBookReturns400ForOptionPointingAtMissingSection() throws Exception {
        BookImportRequest book = book(
                DESERT,
                new Section(1, "Sand in every direction.", SectionType.BEGIN, List.of(new Option("Walk", 99, null))),
                new Section(2, "An oasis.", SectionType.END, List.of()));

        mockMvc.perform(addRequest(book))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Section 1 references missing section 99")));

        assertNoBookWasAdded();
    }

    @Test
    void addBookReturns400WhenThereIsNoBeginSection() throws Exception {
        BookImportRequest book = book(DESERT, new Section(1, "An oasis.", SectionType.END, List.of()));

        mockMvc.perform(addRequest(book))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Expected 1 BEGIN section but got: 0")));

        assertNoBookWasAdded();
    }

    @Test
    void addBookReturns400ForMalformedBody() throws Exception {
        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());

        assertNoBookWasAdded();
    }

    // Book categories

    @Test
    void addBookCategoryAddsTheCategoryToTheBook() throws Exception {
        mockMvc.perform(put(CATEGORY_PATH, cavernsId, "fantasy")).andExpect(status().isOk());

        mockMvc.perform(get(BOOK_PATH, cavernsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", contains("fantasy")));
        mockMvc.perform(get("/books").param("category", "fantasy"))
                .andExpect(jsonPath("$._embedded.books.length()").value(1))
                .andExpect(jsonPath("$._embedded.books[0].title").value(CAVERNS));
    }

    @Test
    void addBookCategoryDoesNotAddTheSameCategoryTwiceWhateverTheCasing() throws Exception {
        mockMvc.perform(put(CATEGORY_PATH, towerId, "mystery")).andExpect(status().isOk());
        mockMvc.perform(put(CATEGORY_PATH, towerId, "Mystery")).andExpect(status().isOk());
        mockMvc.perform(put(CATEGORY_PATH, towerId, "MYSTERY")).andExpect(status().isOk());

        mockMvc.perform(get(BOOK_PATH, towerId)).andExpect(jsonPath("$.categories", contains("mystery")));
    }

    @Test
    void addBookCategoryKeepsExistingCategories() throws Exception {
        mockMvc.perform(put(CATEGORY_PATH, towerId, "Horror")).andExpect(status().isOk());

        mockMvc.perform(get(BOOK_PATH, towerId)).andExpect(jsonPath("$.categories", contains("mystery", "Horror")));
    }

    @Test
    void addBookCategoryReturns400ForBlankCategory() throws Exception {
        mockMvc.perform(put(CATEGORY_PATH, towerId, " ")).andExpect(status().isBadRequest());

        mockMvc.perform(get(BOOK_PATH, towerId)).andExpect(jsonPath("$.categories", contains("mystery")));
    }

    @Test
    void addBookCategoryReturns404ForUnknownBook() throws Exception {
        mockMvc.perform(put(CATEGORY_PATH, UNKNOWN_BOOK_ID, "fantasy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_BOOK_ID))));
    }

    @Test
    void removeBookCategoryRemovesTheCategoryWhateverTheCasing() throws Exception {
        mockMvc.perform(put(CATEGORY_PATH, towerId, "Horror")).andExpect(status().isOk());

        mockMvc.perform(delete(CATEGORY_PATH, towerId, "MYSTERY")).andExpect(status().isOk());

        mockMvc.perform(get(BOOK_PATH, towerId)).andExpect(jsonPath("$.categories", contains("Horror")));
        mockMvc.perform(get("/books").param("category", "mystery"))
                .andExpect(jsonPath("$._embedded.books").doesNotExist());
    }

    @Test
    void removeBookCategoryLeavesTheBookUnchangedWhenItDoesNotHaveTheCategory() throws Exception {
        mockMvc.perform(delete(CATEGORY_PATH, towerId, "fantasy")).andExpect(status().isOk());

        mockMvc.perform(get(BOOK_PATH, towerId)).andExpect(jsonPath("$.categories", contains("mystery")));
    }

    @Test
    void removeBookCategoryReturns404ForUnknownBook() throws Exception {
        mockMvc.perform(delete(CATEGORY_PATH, UNKNOWN_BOOK_ID, "mystery"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString(String.valueOf(UNKNOWN_BOOK_ID))));
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

    // Begins on section 5 so its begin link differs from the caverns book, which begins on section 1.
    private void importBook(String title, String author, List<String> categories, Difficulty difficulty) {
        bookImportService.importBook(towerBook(title, author, categories, difficulty));
    }

    private static BookImportRequest towerBook(String title, String author, List<String> categories) {
        return towerBook(title, author, categories, Difficulty.MEDIUM);
    }

    private static BookImportRequest towerBook(
            String title, String author, List<String> categories, Difficulty difficulty) {
        return new BookImportRequest(
                title,
                author,
                categories,
                difficulty,
                List.of(
                        new Section(
                                5,
                                "You wake at the foot of a drowned tower.",
                                SectionType.BEGIN,
                                List.of(new Option("Climb the stairs", 6, List.of()))),
                        new Section(6, "The tower top opens to the sky.", SectionType.END, List.of())));
    }

    private static BookImportRequest book(String title, Section... sections) {
        return new BookImportRequest(title, "Idris Vale", List.of(), Difficulty.MEDIUM, List.of(sections));
    }

    private MockHttpServletRequestBuilder addRequest(BookImportRequest book) {
        return post("/books").contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(book));
    }

    private void assertNoBookWasAdded() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(jsonPath("$._embedded.books.length()").value(2));
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
}
