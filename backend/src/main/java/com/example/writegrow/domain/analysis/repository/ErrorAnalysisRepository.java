package com.example.writegrow.domain.analysis.repository;

import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorAnalysisRepository extends JpaRepository<ErrorAnalysis, Long> {

    Optional<ErrorAnalysis> findByWritingId(Long writingId);

    @EntityGraph(attributePaths = "candidates")
    Optional<ErrorAnalysis> findWithCandidatesByWritingId(Long writingId);
}
