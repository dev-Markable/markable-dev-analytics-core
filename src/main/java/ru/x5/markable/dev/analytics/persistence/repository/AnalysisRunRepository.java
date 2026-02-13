package ru.x5.markable.dev.analytics.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.x5.markable.dev.analytics.persistence.entity.AnalysisRun;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, UUID> {

}
