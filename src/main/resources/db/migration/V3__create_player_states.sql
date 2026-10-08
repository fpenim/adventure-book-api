-- One row per player and book: a player can be reading several books at once.
CREATE TABLE player_states (
    username TEXT NOT NULL,
    book_id BIGINT NOT NULL,
    current_section_id INTEGER NOT NULL,
    health INTEGER NOT NULL,

    PRIMARY KEY (username, book_id),

    CONSTRAINT fk_player_states_section
        FOREIGN KEY (book_id, current_section_id)
        REFERENCES sections (book_id, id)
        ON DELETE CASCADE,

    CONSTRAINT chk_player_health
        CHECK (health >= 0)
);
