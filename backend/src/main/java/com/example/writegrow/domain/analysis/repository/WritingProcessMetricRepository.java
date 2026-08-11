package com.example.writegrow.domain.analysis.repository;

import com.example.writegrow.domain.analysis.entity.WritingProcessMetric;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WritingProcessMetricRepository extends JpaRepository<WritingProcessMetric, Long> {

    Optional<WritingProcessMetric> findByWritingId(Long writingId);
}
