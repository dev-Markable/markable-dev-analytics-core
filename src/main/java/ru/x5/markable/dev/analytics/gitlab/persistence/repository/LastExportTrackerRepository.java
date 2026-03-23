package ru.x5.markable.dev.analytics.gitlab.persistence.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.LastExportTracker;

@Repository
public interface LastExportTrackerRepository extends JpaRepository<LastExportTracker, UUID> {

    Optional<LastExportTracker> findByExportType(String exportType);

    @Query("SELECT l.lastExportTime FROM LastExportTracker l WHERE l.exportType = :type")
    Optional<LocalDateTime> findLastExportTimeByType(@Param("type") String type);

}
