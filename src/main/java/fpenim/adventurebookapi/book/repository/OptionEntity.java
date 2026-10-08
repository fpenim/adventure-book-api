package fpenim.adventurebookapi.book.repository;

import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.model.section.option.Option;
import fpenim.adventurebookapi.book.repository.SectionEntity.SectionId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "options")
public class OptionEntity {

    @EmbeddedId
    private OptionId id;

    @MapsId("sectionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", referencedColumnName = "book_id", nullable = false)
    @JoinColumn(name = "section_id", referencedColumnName = "id", nullable = false)
    private SectionEntity section;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "go_to", nullable = false)
    private Integer goTo;

    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL)
    @OrderBy("id.position")
    private List<ConsequenceEntity> consequences = new ArrayList<>();

    protected OptionEntity() {
        // Required by JPA.
    }

    OptionEntity(SectionEntity section, int position, Option option) {
        this.section = Objects.requireNonNull(section);
        this.id = new OptionId(new SectionId(section.getBook().getId(), section.getId()), position);
        this.description = Objects.requireNonNull(option.description());
        this.goTo = option.gotoId();

        List<Consequence> optionConsequences = option.consequences();
        for (int i = 0; i < optionConsequences.size(); i++) {
            this.consequences.add(new ConsequenceEntity(this, i, optionConsequences.get(i)));
        }
    }

    public OptionId getId() {
        return id;
    }

    public Integer getPosition() {
        return id.getPosition();
    }

    public SectionEntity getSection() {
        return section;
    }

    public String getDescription() {
        return description;
    }

    public Integer getGoTo() {
        return goTo;
    }

    public List<ConsequenceEntity> getConsequences() {
        return List.copyOf(consequences);
    }

    @Embeddable
    public static class OptionId implements Serializable {

        // Columns (book_id, section_id) come from the section association via @MapsId.
        private SectionId sectionId;

        @Column(name = "position")
        private Integer position;

        protected OptionId() {
            // Required by JPA.
        }

        public OptionId(SectionId sectionId, Integer position) {
            this.sectionId = sectionId;
            this.position = position;
        }

        public SectionId getSectionId() {
            return sectionId;
        }

        public Integer getPosition() {
            return position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof OptionId other)) {
                return false;
            }
            return Objects.equals(sectionId, other.sectionId) && Objects.equals(position, other.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sectionId, position);
        }
    }
}
