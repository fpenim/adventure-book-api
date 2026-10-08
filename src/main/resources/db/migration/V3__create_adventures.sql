-- A user may have several adventures at once, on the same book or on different books.
CREATE TABLE adventures (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username TEXT NOT NULL,
    book_id BIGINT NOT NULL,
    current_section_id INTEGER NOT NULL,
    health INTEGER NOT NULL,
    CONSTRAINT fk_adventures_section FOREIGN KEY (book_id, current_section_id) REFERENCES sections (book_id, id) ON DELETE CASCADE,
    CONSTRAINT chk_adventure_health CHECK (health >= 0)
);

CREATE INDEX idx_adventures_username ON adventures (username);
