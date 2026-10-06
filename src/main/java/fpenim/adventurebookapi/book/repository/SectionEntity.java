package fpenim.adventurebookapi.book.repository;

import fpenim.adventurebookapi.book.model.section.Section;
import fpenim.adventurebookapi.book.model.section.SectionType;
import fpenim.adventurebookapi.book.model.section.option.Option;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "sections")
public class SectionEntity {

    @EmbeddedId
    private SectionId id;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 10)
    private SectionType type;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL)
    @OrderBy("id.position")
    private List<OptionEntity> options = new ArrayList<>();

    protected SectionEntity() {
        // Required by JPA.
    }

    SectionEntity(BookEntity book, Section section) {
        this.book = Objects.requireNonNull(book);
        this.id = new SectionId(book.getId(), section.id());
        this.text = Objects.requireNonNull(section.text());
        this.type = Objects.requireNonNull(section.type());

        List<Option> sectionOptions = section.options();
        for (int position = 0; position < sectionOptions.size(); position++) {
            this.options.add(new OptionEntity(this, position, sectionOptions.get(position)));
        }
    }

    public SectionId getId() {
        return id;
    }

    public Integer getNumber() {
        return id.getNumber();
    }

    public BookEntity getBook() {
        return book;
    }

    public String getText() {
        return text;
    }

    public SectionType getType() {
        return type;
    }

    public List<OptionEntity> getOptions() {
        return List.copyOf(options);
    }

    @Embeddable
    public static class SectionId implements Serializable {

        // Filled in from the book association by @MapsId when the section is persisted.
        @Column(name = "book_id")
        private Long bookId;

        @Column(name = "id")
        private Integer number;

        protected SectionId() {
            // Required by JPA.
        }

        public SectionId(Long bookId, Integer number) {
            this.bookId = bookId;
            this.number = number;
        }

        public Long getBookId() {
            return bookId;
        }

        public Integer getNumber() {
            return number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SectionId other)) {
                return false;
            }
            return Objects.equals(bookId, other.bookId) && Objects.equals(number, other.number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookId, number);
        }
    }
}
