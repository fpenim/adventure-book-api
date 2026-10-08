package fpenim.adventurebookapi.book.repository;

import fpenim.adventurebookapi.book.model.Difficulty;
import fpenim.adventurebookapi.book.model.section.Section;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private Difficulty difficulty;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> categories = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<SectionEntity> sections = new ArrayList<>();

    protected BookEntity() {
        // Required by JPA.
    }

    public BookEntity(
            String title, String author, Difficulty difficulty, List<String> categories, List<Section> sections) {
        this.title = Objects.requireNonNull(title);
        this.author = Objects.requireNonNull(author);
        this.difficulty = Objects.requireNonNull(difficulty);
        this.categories = new ArrayList<>(List.copyOf(categories));

        for (Section section : sections) {
            this.sections.add(new SectionEntity(this, section));
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public List<String> getCategories() {
        return List.copyOf(categories);
    }

    public void addCategory(String category) {
        Objects.requireNonNull(category);

        if (category.isBlank()) {
            throw new IllegalArgumentException("Category must not be blank.");
        }

        if (categories.stream().noneMatch(category::equalsIgnoreCase)) {
            categories.add(category);
        }
    }

    public void removeCategory(String category) {
        categories.removeIf(existing -> existing.equalsIgnoreCase(category));
    }

    public List<SectionEntity> getSections() {
        return List.copyOf(sections);
    }
}
