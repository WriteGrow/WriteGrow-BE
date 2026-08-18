package com.example.writegrow.domain.analysis.repository;

import com.example.writegrow.domain.analysis.entity.ErrorProfile;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorProfileRepository extends JpaRepository<ErrorProfile, Long> {

    Optional<ErrorProfile> findByProfileIdAndErrorType(Long profileId, ErrorType errorType);

    /**
     * 반복이 잦은 순으로 돌려준다. "주요 반복 오류"를 뽑을 때 그대로 쓴다.
     * 페이지네이션을 두지 않는 것은 유형이 여섯 개로 고정이기 때문이다.
     */
    List<ErrorProfile> findAllByProfileIdOrderByOccurrenceCountDesc(Long profileId);
}
