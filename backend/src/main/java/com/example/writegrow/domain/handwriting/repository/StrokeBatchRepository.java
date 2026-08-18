package com.example.writegrow.domain.handwriting.repository;

import com.example.writegrow.domain.handwriting.entity.StrokeBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 획 배치는 시도별로 나뉜다. 다시 쓴 글은 이전 시도의 배치를 그대로 두고 시도 번호만 올리므로,
 * 조회는 반드시 시도 번호까지 함께 지정해야 한다. 그러지 않으면 폐기한 시도의 획이 분석에 섞인다.
 */
public interface StrokeBatchRepository extends JpaRepository<StrokeBatch, Long> {

    boolean existsByWritingIdAndAttemptNoAndBatchSeq(Long writingId, int attemptNo, int batchSeq);

    List<StrokeBatch> findAllByWritingIdAndAttemptNoOrderByBatchSeqAsc(Long writingId, int attemptNo);

    long countByWritingIdAndAttemptNo(Long writingId, int attemptNo);
}
