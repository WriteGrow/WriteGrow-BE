package com.example.writegrow.domain.analysis.repository;

import com.example.writegrow.domain.analysis.entity.OcrResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

    Optional<OcrResult> findByWritingId(Long writingId);

    @EntityGraph(attributePaths = "segments")
    Optional<OcrResult> findWithSegmentsByWritingId(Long writingId);
}
