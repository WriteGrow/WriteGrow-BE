package com.example.writegrow.domain.writing.repository;

import com.example.writegrow.domain.writing.entity.Writing;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
