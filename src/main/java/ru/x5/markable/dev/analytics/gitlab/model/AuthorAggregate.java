package ru.x5.markable.dev.analytics.gitlab.model;

public record AuthorAggregate(String email,
                               long commits,
                               long added,
                               long deleted) {

    public AuthorAggregate(String email) {
        this(email, 0, 0, 0);
    }

    public AuthorAggregate add(long add, long del) {
        return new AuthorAggregate(email, commits + 1, added + add, deleted + del);
    }

    public AuthorAggregate merge(AuthorAggregate other) {
        return new AuthorAggregate(email,
                commits + other.commits,
                added + other.added,
                deleted + other.deleted);
    }
}
