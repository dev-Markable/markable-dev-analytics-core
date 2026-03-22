package ru.x5.markable.dev.analytics.gitlab.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.DailyAuthorStats;

@Repository
public interface DailyAuthorStatsRepository extends JpaRepository<DailyAuthorStats, Long> {

    Optional<DailyAuthorStats> findByEmailAndDate(String email, LocalDate date);

    List<DailyAuthorStats> findByDateBetween(LocalDate start, LocalDate end);

    List<DailyAuthorStats> findByDate(LocalDate date);

    List<DailyAuthorStats> findByEmailInAndDate(List<String> emails, LocalDate date);

    @Modifying
    @Query("DELETE FROM DailyAuthorStats d WHERE d.date < :date")
    int deleteOlderThan(@Param("date") LocalDate date);

}
