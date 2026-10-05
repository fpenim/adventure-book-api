package fpenim.adventurebookapi.book.repository;

import fpenim.adventurebookapi.book.repository.OptionEntity.OptionId;
import fpenim.adventurebookapi.book.repository.SectionEntity.SectionId;
import fpenim.adventurebookapi.book.model.section.option.Consequence;
import fpenim.adventurebookapi.book.model.section.option.ConsequenceType;
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
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Immutable
@Table(name = "consequences")
public class ConsequenceEntity {

    @EmbeddedId
    private ConsequenceId id;

    @MapsId("optionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", referencedColumnName = "book_id", nullable = false)
    @JoinColumn(name = "section_id", referencedColumnName = "section_id", nullable = false)
    @JoinColumn(name = "option_position", referencedColumnName = "position", nullable = false)
    private OptionEntity option;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsequenceType type;

    @Column(nullable = false)
    private Integer value;

    @Column(columnDefinition = "text")
    private String text;

    protected ConsequenceEntity() {
        // Required by JPA.
    }

    ConsequenceEntity(OptionEntity option, int position, Consequence consequence) {
        this.option = Objects.requireNonNull(option);

        SectionId sectionId = option.getId().getSectionId();
        this.id = new ConsequenceId(
                new OptionId(
                        new SectionId(sectionId.getBookId(), sectionId.getNumber()),
                        option.getPosition()
                ),
                position
        );
        this.type = Objects.requireNonNull(consequence.type());
        this.value = consequence.value();
        this.text = consequence.text();
    }

    public ConsequenceId getId() {
        return id;
    }

    public Integer getPosition() {
        return id.getPosition();
    }

    public OptionEntity getOption() {
        return option;
    }

    public ConsequenceType getType() {
        return type;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    @Embeddable
    public static class ConsequenceId implements Serializable {

        // Columns (book_id, section_id, option_position) come from the option association via @MapsId.
        private OptionId optionId;

        @Column(name = "position")
        private Integer position;

        protected ConsequenceId() {
            // Required by JPA.
        }

        public ConsequenceId(OptionId optionId, Integer position) {
            this.optionId = optionId;
            this.position = position;
        }

        public OptionId getOptionId() {
            return optionId;
        }

        public Integer getPosition() {
            return position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConsequenceId other)) {
                return false;
            }
            return Objects.equals(optionId, other.optionId)
                    && Objects.equals(position, other.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(optionId, position);
        }
    }
}
