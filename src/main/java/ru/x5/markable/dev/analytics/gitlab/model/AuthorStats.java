package ru.x5.markable.dev.analytics.gitlab.model;

import java.util.HashSet;
import java.util.Set;

public class AuthorStats {

    private String email;
    private long commits;
    private long added;
    private long deleted;
    private Set<String> repositories = new HashSet<>();

    public void addCommit(long add, long del, String repo) {
        this.commits++;
        this.added += add;
        this.deleted += del;
        this.repositories.add(repo);
    }

}
