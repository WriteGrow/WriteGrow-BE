package com.example.writegrow.domain.analysis.repository;

import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErrorAnalysisRepository extends JpaRepository<ErrorAnalysis, Long> {

    Optional<ErrorAnalysis> findByWritingId(Long writingId);

    @EntityGraph(attributePaths = "candidates")
    Optional<ErrorAnalysis> findWithCandidatesByWritingId(Long writingId);

    /**
     * 기간 내 분석 결과를 후보까지 함께 조회한다. 주간 리포트가 확정 오류 수와 검토 대기 건수를
     * 이 결과에서 함께 계산한다.
     *
     * <p>기간 기준은 분석 완료 시각이다. 글 작성 시각을 쓰면 주 경계에 걸친 글의 분석이 다음 주로
     * 넘어갔을 때 어느 주에도 잡히지 않는다.
     */
    @Query("""
            select a from ErrorAnalysis a
            where a.profileId = :profileId
              and a.completedAt >= :start and a.completedAt < :end
            """)
    @EntityGraph(attributePaths = "candidates")
    List<ErrorAnalysis> findAllWithCandidatesInPeriod(@Param("profileId") Long profileId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}
