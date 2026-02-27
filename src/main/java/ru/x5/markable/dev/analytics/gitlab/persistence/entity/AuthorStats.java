package ru.x5.markable.dev.analytics.gitlab.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "author_stats",
        indexes = {
                @Index(name = "idx_author_analysis", columnList = "analysis_id"),
                @Index(name = "idx_author_email", columnList = "email")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private UUID analysisId;

    @Column(nullable = false)
    private String email;

    @Column(name = "merge_commits")
    long mergeCommits;

    @Column(nullable = false)
    private Long commits;

    @Column(name = "added_lines", nullable = false)
    private Long addedLines;

    @Column(name = "deleted_lines", nullable = false)
    private Long deletedLines;

    @Column(name = "test_added_lines")
    private Long testAddedLines;
}

