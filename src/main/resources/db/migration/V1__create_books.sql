CREATE TABLE books (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    difficulty VARCHAR(10) NOT NULL
       CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    categories TEXT[] NOT NULL DEFAULT '{}'
);
