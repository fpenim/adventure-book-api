package fpenim.adventurebookapi.book.repository;

import static fpenim.adventurebookapi.jooq.Tables.BOOKS;
import static fpenim.adventurebookapi.jooq.Tables.SECTIONS;
import static org.jooq.impl.DSL.array;
import static org.jooq.impl.DSL.arrayAppend;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.unnest;
import static org.jooq.impl.DSL.when;

import fpenim.adventurebookapi.book.BookFilter;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import fpenim.adventurebookapi.book.dto.BookSummaryResponse;
import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.model.section.option.Option;
import fpenim.adventurebookapi.jooq.tables.records.ConsequencesRecord;
import fpenim.adventurebookapi.jooq.tables.records.OptionsRecord;
import fpenim.adventurebookapi.jooq.tables.records.SectionsRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record6;
import org.jooq.SelectConditionStep;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

    private static final Field<String> CATEGORY = field(name("category_item", "value"), String.class);
    private static final Field<Long> CATEGORY_ORDINAL = field(name("category_item", "ordinal"), Long.class);

    private final DSLContext dsl;

    public BookRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<BookSummaryResponse> findAll(BookFilter filter) {
        return selectSummaries()
                .and(titleContains(filter.title()))
                .and(authorContains(filter.author()))
                .and(hasCategory(filter.category()))
                .and(hasDifficulty(filter.difficulty()))
                .orderBy(BOOKS.ID)
                .fetch(BookRepository::toSummary);
    }

    public Optional<BookSummaryResponse> findById(Long id) {
        return selectSummaries().and(BOOKS.ID.eq(id)).fetchOptional(BookRepository::toSummary);
    }

    /** Returns false when the book does not exist. */
    public boolean addCategory(Long id, String category) {
        return dsl.update(BOOKS)
                        .set(
                                BOOKS.CATEGORIES,
                                when(containsCategory(category), BOOKS.CATEGORIES)
                                        .otherwise(arrayAppend(BOOKS.CATEGORIES, category)))
                        .where(BOOKS.ID.eq(id))
                        .execute()
                > 0;
    }

    /** Returns false when the book does not exist. */
    public boolean removeCategory(Long id, String category) {
        Table<?> categories = unnest(BOOKS.CATEGORIES).withOrdinality().as("category_item", "value", "ordinal");

        return dsl.update(BOOKS)
                        .set(
                                BOOKS.CATEGORIES,
                                array(select(CATEGORY)
                                        .from(categories)
                                        .where(lower(CATEGORY).ne(normalize(category)))
                                        .orderBy(CATEGORY_ORDINAL)))
                        .where(BOOKS.ID.eq(id))
                        .execute()
                > 0;
    }

    public Long insert(BookImportRequest book) {
        Long bookId = dsl.insertInto(BOOKS)
                .set(BOOKS.TITLE, book.title())
                .set(BOOKS.AUTHOR, book.author())
                .set(BOOKS.DIFFICULTY, book.difficulty())
                .set(BOOKS.CATEGORIES, book.categories().toArray(String[]::new))
                .returningResult(BOOKS.ID)
                .fetchSingle(BOOKS.ID);

        List<SectionsRecord> sections = new ArrayList<>();
        List<OptionsRecord> options = new ArrayList<>();
        List<ConsequencesRecord> consequences = new ArrayList<>();

        for (Section section : book.sections()) {
            sections.add(new SectionsRecord(bookId, section.id(), section.text(), section.type()));

            List<Option> sectionOptions = section.options();
            for (int position = 0; position < sectionOptions.size(); position++) {
                Option option = sectionOptions.get(position);
                options.add(new OptionsRecord(bookId, section.id(), position, option.description(), option.gotoId()));

                List<Consequence> optionConsequences = option.consequences();
                for (int i = 0; i < optionConsequences.size(); i++) {
                    Consequence consequence = optionConsequences.get(i);
                    consequences.add(new ConsequencesRecord(
                            bookId,
                            section.id(),
                            position,
                            i,
                            consequence.type(),
                            consequence.value(),
                            consequence.text()));
                }
            }
        }

        // Children reference their parents, so the order matters.
        dsl.batchInsert(sections).execute();
        dsl.batchInsert(options).execute();
        dsl.batchInsert(consequences).execute();

        return bookId;
    }

    // Every book has exactly one BEGIN section, enforced on import.
    private SelectConditionStep<Record6<Long, String, String, String[], Difficulty, Integer>> selectSummaries() {
        return dsl.select(BOOKS.ID, BOOKS.TITLE, BOOKS.AUTHOR, BOOKS.CATEGORIES, BOOKS.DIFFICULTY, SECTIONS.ID)
                .from(BOOKS)
                .join(SECTIONS)
                .on(SECTIONS.BOOK_ID.eq(BOOKS.ID))
                .where(SECTIONS.SECTION_TYPE.eq(SectionType.BEGIN));
    }

    private static BookSummaryResponse toSummary(Record6<Long, String, String, String[], Difficulty, Integer> row) {
        return new BookSummaryResponse(
                row.value1(), row.value2(), row.value3(), List.of(row.value4()), row.value5(), row.value6());
    }

    private static Condition titleContains(String title) {
        return isBlank(title) ? noCondition() : lower(BOOKS.TITLE).like(containsPattern(title), '!');
    }

    private static Condition authorContains(String author) {
        return isBlank(author) ? noCondition() : lower(BOOKS.AUTHOR).like(containsPattern(author), '!');
    }

    private static Condition hasDifficulty(Difficulty difficulty) {
        return difficulty == null ? noCondition() : BOOKS.DIFFICULTY.eq(difficulty);
    }

    private static Condition hasCategory(String category) {
        return isBlank(category) ? noCondition() : containsCategory(category.strip());
    }

    private static Condition containsCategory(String category) {
        return exists(selectOne()
                .from(unnest(BOOKS.CATEGORIES).as("category_item", "value"))
                .where(lower(CATEGORY).eq(normalize(category))));
    }

    private static String normalize(String category) {
        return category.toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String containsPattern(String input) {
        String escaped = input.strip()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }
}
