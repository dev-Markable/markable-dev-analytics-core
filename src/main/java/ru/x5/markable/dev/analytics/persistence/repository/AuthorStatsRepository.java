package ru.x5.markable.dev.analytics.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.x5.markable.dev.analytics.persistence.entity.AuthorStats;

public interface AuthorStatsRepository extends JpaRepository<AuthorStats, Long> {

}
