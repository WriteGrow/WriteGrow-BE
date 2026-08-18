package com.example.writegrow.domain.report.service;

import com.example.writegrow.domain.report.dto.response.ChildWritingDetailResponse;
import com.example.writegrow.domain.report.dto.response.ParentHomeResponse;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.global.common.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

/**
 * 보호자 주간 성장 리포트와 글 기록 열람. (기능명세서 REQ-06)
 */
public interface WeeklyReportService {

    /**
     * 보호자 홈. 조회자와 같은 계정에 속한 아동들의 이번 주 현황을 돌려준다.
     */
    ParentHomeResponse getParentHome(Long viewerProfileId);

    /**
     * 주간 성장 리포트.
     *
     * @param weekOf 조회할 주에 속한 아무 날짜. null 이면 이번 주.
     */
    WeeklyReportResponse getWeeklyReport(Long viewerProfileId, Long childProfileId, LocalDate weekOf);

    /**
     * 자녀의 글 목록. 계속 쌓이는 목록이라 페이지네이션을 적용한다.
     */
    PageResponse<WritingSummaryResponse> getChildWritings(Long viewerProfileId, Long childProfileId,
                                                          Pageable pageable);

    /**
     * 자녀의 개별 글 수정 전후 열람.
     */
    ChildWritingDetailResponse getChildWriting(Long viewerProfileId, Long childProfileId, Long writingId);
}
