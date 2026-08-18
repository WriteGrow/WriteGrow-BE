package com.example.writegrow.domain.writing.repository;

import com.example.writegrow.domain.writing.entity.Writing;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WritingRepository extends JpaRepository<Writing, Long> {

    /**
     * 수정 이력까지 함께 조회한다. 상세 조회와 최종본 확정에서 사용한다.
     */
    @EntityGraph(attributePaths = "revisions")
    Optional<Writing> findWithRevisionsById(Long id);

    /**
     * 정렬은 메서드 이름에 고정하지 않고 {@link Pageable} 이 결정한다.
     * 이름에 OrderBy 를 두면 Pageable 의 정렬과 겹쳐 ORDER BY 절이 중복된다. 컨트롤러의 기본값은 최신순이다.
     */
    Page<Writing> findAllByProfileId(Long profileId, Pageable pageable);

    /**
     * 기간 내 글을 수정 이력까지 함께 조회한다. 주간 리포트가 작성 횟수·자기교정 횟수·일자별 추이를
     * 모두 이 결과 하나로 계산한다.
     *
     * <p>집계 쿼리를 여러 개 두는 대신 한 번에 읽어 메모리에서 묶는다. 한 주에 쌓이는 글은 많아야
     * 십여 건이라 이 편이 단순하고, 지표를 하나 추가할 때마다 쿼리가 늘지 않는다.
     *
     * <p>경계는 {@code [start, end)} 다. {@code Between} 은 양끝을 포함해 주 경계에서 하루가 겹친다.
     */
    @Query("""
            select w from Writing w
            where w.profileId = :profileId
              and w.createdAt >= :start and w.createdAt < :end
            order by w.createdAt asc
            """)
    @EntityGraph(attributePaths = "revisions")
    List<Writing> findAllInPeriod(@Param("profileId") Long profileId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * 연속 작성 일수를 세기 위한 작성 시각 목록. 최근 것부터 돌려준다.
     */
    @Query("""
            select w.createdAt from Writing w
            where w.profileId = :profileId and w.createdAt >= :since
            order by w.createdAt desc
            """)
    List<LocalDateTime> findCreatedAtSince(@Param("profileId") Long profileId,
                                           @Param("since") LocalDateTime since);
}
