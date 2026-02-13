package ru.x5.markable.dev.analytics.gitlab.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.RepoStats;

public interface RepoStatsRepository extends JpaRepository<RepoStats, Long> {

}
