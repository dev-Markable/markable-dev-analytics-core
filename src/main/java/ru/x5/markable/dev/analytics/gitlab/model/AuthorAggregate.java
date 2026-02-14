package ru.x5.markable.dev.analytics.gitlab.model;

public record AuthorAggregate(String email,
                               long mergeCommits,
                               long commits,
                               long added,
                               long deleted,
                               long testAdded) {

    public AuthorAggregate(String email) {
        this(email, 0, 0, 0, 0, 0);
    }

    public AuthorAggregate addCommit(boolean isMerge) {
        return new AuthorAggregate(
                email,
                mergeCommits + (isMerge ? 1 : 0),
                commits + (isMerge ? 0 : 1),
                added,
                deleted,
                testAdded
        );
    }

    public AuthorAggregate addLines(long add, long del, boolean isTest) {
        return new AuthorAggregate(
                email,
                mergeCommits,
                commits,
                added + add,
                deleted + del,
                testAdded + (isTest ? add : 0)
        );
    }

    public AuthorAggregate merge(AuthorAggregate other) {
        return new AuthorAggregate(
                email,
                mergeCommits + other.mergeCommits,
                commits + other.commits,
                added + other.added,
                deleted + other.deleted,
                testAdded + other.testAdded
        );
    }
}
