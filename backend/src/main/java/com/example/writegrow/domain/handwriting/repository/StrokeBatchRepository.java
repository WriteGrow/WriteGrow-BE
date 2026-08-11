package com.example.writegrow.domain.handwriting.repository;

import com.example.writegrow.domain.handwriting.entity.StrokeBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrokeBatchRepository extends JpaRepository<StrokeBatch, Long> {

    boolean existsByWritingIdAndBatchSeq(Long writingId, int batchSeq);

    List<StrokeBatch> findAllByWritingIdOrderByBatchSeqAsc(Long writingId);

    long countByWritingId(Long writingId);
}
