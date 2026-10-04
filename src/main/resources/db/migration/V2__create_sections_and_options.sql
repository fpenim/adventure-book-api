CREATE TABLE sections (
    book_id BIGINT NOT NULL,
    id INTEGER NOT NULL,
    text TEXT NOT NULL,
    section_type VARCHAR(10) NOT NULL,

    PRIMARY KEY (book_id, id),

    CONSTRAINT fk_sections_book
        FOREIGN KEY (book_id)
        REFERENCES books (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_section_type
        CHECK (section_type IN ('BEGIN', 'NODE', 'END'))
);

-- A book can have at most one BEGIN section.
CREATE UNIQUE INDEX uq_sections_begin_per_book
    ON sections (book_id)
    WHERE section_type = 'BEGIN';

CREATE TABLE options (
    book_id BIGINT NOT NULL,
    section_id INTEGER NOT NULL,
    position INTEGER NOT NULL,
    description TEXT NOT NULL,
    go_to INTEGER NOT NULL,

    PRIMARY KEY (book_id, section_id, position),

    CONSTRAINT fk_options_source
        FOREIGN KEY (book_id, section_id)
        REFERENCES sections (book_id, id)
        ON DELETE CASCADE,

    CONSTRAINT fk_options_destination
        FOREIGN KEY (book_id, go_to)
        REFERENCES sections (book_id, id),

    CONSTRAINT chk_option_position
        CHECK (position >= 0)
);

CREATE INDEX idx_options_destination
    ON options (book_id, go_to);

CREATE TABLE consequences (
    book_id BIGINT NOT NULL,
    section_id INTEGER NOT NULL,
    option_position INTEGER NOT NULL,
    position INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    value INTEGER NOT NULL,
    text TEXT,

    PRIMARY KEY (
        book_id,
        section_id,
        option_position,
        position
    ),

    CONSTRAINT fk_consequences_option
        FOREIGN KEY (book_id, section_id, option_position)
        REFERENCES options (book_id, section_id, position)
        ON DELETE CASCADE,

    CONSTRAINT chk_consequence_position
        CHECK (position >= 0),

    CONSTRAINT chk_consequence_type
        CHECK (type IN ('GAIN_HEALTH', 'LOSE_HEALTH')),

    CONSTRAINT chk_consequence_value
        CHECK (value > 0)
);
