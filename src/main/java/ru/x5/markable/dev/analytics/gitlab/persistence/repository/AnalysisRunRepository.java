package ru.x5.markable.dev.analytics.gitlab.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AnalysisRun;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, UUID> {

}
