package com.example.writegrow.domain.report.service;

import com.example.writegrow.domain.account.entity.Profile;
import com.example.writegrow.domain.account.entity.ProfileRole;
import com.example.writegrow.domain.account.exception.AccountErrorCode;
import com.example.writegrow.domain.account.exception.AccountException;
import com.example.writegrow.domain.account.repository.ProfileRepository;
import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.dto.response.ErrorCandidateResponse;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import com.example.writegrow.domain.analysis.entity.ErrorProfile;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import com.example.writegrow.domain.analysis.repository.ErrorAnalysisRepository;
import com.example.writegrow.domain.analysis.repository.ErrorProfileRepository;
import com.example.writegrow.domain.report.WeekRange;
import com.example.writegrow.domain.report.dto.response.ChildWritingDetailResponse;
import com.example.writegrow.domain.report.dto.response.ParentHomeResponse;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse.DailyTrend;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse.FocusArea;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse.FocusReason;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse.RepeatedError;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse.Summary;
import com.example.writegrow.domain.writing.dto.response.WritingRevisionResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.domain.writing.entity.RevisionSource;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.global.common.PageResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리포트는 여러 도메인의 기록을 읽어 요약만 한다. 자기 엔티티가 없어 entity·repository 패키지를
 * 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {

    /** 연속 작성 일수를 세기 위해 거슬러 올라갈 범위. */
    private static final int STREAK_LOOKBACK_DAYS = 60;

    /** 보호자 홈 카드에 노출할 주요 오류 유형 수. */
    private static final int TOP_ERROR_TYPES = 2;

    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final WritingRepository writingRepository;
    private final ErrorAnalysisRepository errorAnalysisRepository;
    private final ErrorProfileRepository errorProfileRepository;
    private final ActivityEventService activityEventService;

    @Override
    @Transactional(readOnly = true)
    public ParentHomeResponse getParentHome(Long viewerProfileId) {
        Profile viewer = profileRepository.findById(viewerProfileId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.PROFILE_NOT_FOUND));
        Long accountId = viewer.getAccount().getId();

        WeekRange thisWeek = WeekRange.current();
        List<ParentHomeResponse.ChildCard> cards = profileRepository
                .findAllByAccountIdAndRoleOrderByCreatedAtAscIdAsc(accountId, ProfileRole.CHILD).stream()
                .map(child -> toChildCard(child, thisWeek))
                .toList();

        return new ParentHomeResponse(accountId, cards);
    }

    @Override
    @Transactional
    public WeeklyReportResponse getWeeklyReport(Long viewerProfileId, Long childProfileId, LocalDate weekOf) {
        Profile child = profileService.getViewableChild(viewerProfileId, childProfileId);
        WeekRange week = weekOf == null ? WeekRange.current() : WeekRange.of(weekOf);
        WeekRange previous = week.previous();

        List<Writing> writings = findWritings(childProfileId, week);
        List<ErrorAnalysis> analyses = findAnalyses(childProfileId, week);
        List<ErrorProfile> profiles =
                errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(childProfileId);

        long confirmedErrors = countConfirmed(analyses);
        long reviewPending = countReviewPending(analyses);
        int selfCorrections = countSelfCorrections(writings);
        int previousSelfCorrections = countSelfCorrections(findWritings(childProfileId, previous));
        long previousErrors = countConfirmed(findAnalyses(childProfileId, previous));

        Summary summary = new Summary(
                writings.size(),
                (int) writings.stream().filter(w -> w.getStatus() == WritingStatus.CONFIRMED).count(),
                selfCorrections,
                selfCorrections - previousSelfCorrections,
                confirmedErrors,
                previousErrors,
                confirmedErrors - previousErrors,
                profiles.size(),
                reviewPending);

        // 명세가 리포트 열람 이벤트 기록을 요구한다. (REQ-06 결과)
        activityEventService.record(ActivityEventType.WEEKLY_REPORT_VIEWED, viewerProfileId, null,
                Map.of("childProfileId", childProfileId, "weekStart", week.start().toString()));

        return new WeeklyReportResponse(
                childProfileId,
                child.getNickname(),
                week.start(),
                week.lastDay(),
                !writings.isEmpty(),
                summary,
                toRepeatedErrors(profiles, analyses, week),
                toDailyTrends(writings, analyses, week),
                toFocusArea(profiles));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WritingSummaryResponse> getChildWritings(Long viewerProfileId, Long childProfileId,
                                                                 Pageable pageable) {
        profileService.getViewableChild(viewerProfileId, childProfileId);
        return PageResponse.of(
                writingRepository.findAllByProfileId(childProfileId, pageable),
                WritingSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ChildWritingDetailResponse getChildWriting(Long viewerProfileId, Long childProfileId,
                                                      Long writingId) {
        Profile child = profileService.getViewableChild(viewerProfileId, childProfileId);

        Writing writing = writingRepository.findWithRevisionsById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        // 경로의 아동과 글의 주인이 다르면 다른 아동의 글을 들여다보는 것이다.
        writing.validateOwner(childProfileId);

        List<ErrorCandidateResponse> confirmedErrors = errorAnalysisRepository
                .findWithCandidatesByWritingId(writingId)
                .map(analysis -> analysis.confirmedCandidates().stream()
                        .map(ErrorCandidateResponse::from)
                        .toList())
                .orElseGet(List::of);
        int reviewPending = errorAnalysisRepository.findWithCandidatesByWritingId(writingId)
                .map(analysis -> analysis.reviewCandidates().size())
                .orElse(0);

        return new ChildWritingDetailResponse(
                writingId,
                childProfileId,
                child.getNickname(),
                writing.getTopic(),
                writing.getInputType(),
                writing.getStatus(),
                writing.getCreatedAt(),
                writing.getSubmittedAt(),
                writing.getOriginalText(),
                writing.getFinalText(),
                countSentences(writing.getFinalText()),
                countSelfCorrections(List.of(writing)),
                writing.getRevisions().stream().map(WritingRevisionResponse::from).toList(),
                confirmedErrors,
                reviewPending);
    }

    private ParentHomeResponse.ChildCard toChildCard(Profile child, WeekRange week) {
        Long childId = child.getId();
        List<Writing> writings = findWritings(childId, week);
        long weeklyErrors = countConfirmed(findAnalyses(childId, week));
        long previousErrors = countConfirmed(findAnalyses(childId, week.previous()));

        Writing recent = writings.isEmpty() ? null : writings.get(writings.size() - 1);
        List<ErrorType> topTypes = errorProfileRepository
                .findAllByProfileIdOrderByOccurrenceCountDesc(childId).stream()
                .limit(TOP_ERROR_TYPES)
                .map(ErrorProfile::getErrorType)
                .toList();

        return new ParentHomeResponse.ChildCard(
                childId,
                child.getNickname(),
                toAge(child.getBirthYear()),
                writings.size(),
                countSelfCorrections(writings),
                countStreakDays(childId),
                recent == null ? null : recent.getId(),
                recent == null ? null : WritingSummaryResponse.from(recent).preview(),
                topTypes,
                weeklyErrors,
                weeklyErrors - previousErrors);
    }

    private List<Writing> findWritings(Long profileId, WeekRange week) {
        return writingRepository.findAllInPeriod(profileId, week.startDateTime(), week.endDateTime());
    }

    private List<ErrorAnalysis> findAnalyses(Long profileId, WeekRange week) {
        return errorAnalysisRepository.findAllWithCandidatesInPeriod(
                profileId, week.startDateTime(), week.endDateTime());
    }

    /**
     * 확정 오류만 센다. 낮은 확신도 후보는 리포트 집계에서 제외한다. (REQ-06 예외 규칙)
     */
    private long countConfirmed(List<ErrorAnalysis> analyses) {
        return analyses.stream().mapToLong(analysis -> analysis.confirmedCandidates().size()).sum();
    }

    private long countReviewPending(List<ErrorAnalysis> analyses) {
        return analyses.stream().mapToLong(analysis -> analysis.reviewCandidates().size()).sum();
    }

    /**
     * 자기교정은 아동이 변환 텍스트를 직접 고친 기록으로 센다. REQ-04(단계적 힌트)가 붙으면
     * 힌트를 보고 고친 횟수가 여기에 더해진다.
     */
    private int countSelfCorrections(List<Writing> writings) {
        return (int) writings.stream()
                .flatMap(writing -> writing.getRevisions().stream())
                .filter(revision -> revision.getSource() == RevisionSource.CHILD_EDIT)
                .count();
    }

    private int countStreakDays(Long profileId) {
        List<LocalDate> dates = writingRepository
                .findCreatedAtSince(profileId, LocalDate.now().minusDays(STREAK_LOOKBACK_DAYS).atStartOfDay())
                .stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .toList();
        if (dates.isEmpty()) {
            return 0;
        }

        // 오늘 아직 안 썼어도 어제까지 이어졌으면 연속으로 본다. 그렇지 않으면 자정마다 0이 된다.
        LocalDate today = LocalDate.now();
        LocalDate expected = dates.get(0);
        if (expected.isBefore(today.minusDays(1))) {
            return 0;
        }

        int streak = 0;
        for (LocalDate date : dates) {
            if (!date.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    private List<RepeatedError> toRepeatedErrors(List<ErrorProfile> profiles,
                                                 List<ErrorAnalysis> analyses, WeekRange week) {
        Map<ErrorType, Long> weeklyCounts = analyses.stream()
                .flatMap(analysis -> analysis.confirmedCandidates().stream())
                .collect(Collectors.groupingBy(candidate -> candidate.getErrorType(), Collectors.counting()));

        return profiles.stream()
                .map(profile -> new RepeatedError(
                        profile.getErrorType(),
                        profile.getErrorType().getLabel(),
                        profile.getOccurrenceCount(),
                        weeklyCounts.getOrDefault(profile.getErrorType(), 0L),
                        profile.getLastOccurredOn()))
                .toList();
    }

    /**
     * 글이 없는 날도 0 으로 채워 돌려준다. 빠뜨리면 화면이 날짜 축을 직접 만들어야 하고,
     * "쓰지 않은 날"과 "데이터가 없는 날"이 구분되지 않는다.
     */
    private List<DailyTrend> toDailyTrends(List<Writing> writings, List<ErrorAnalysis> analyses,
                                           WeekRange week) {
        Map<LocalDate, List<Writing>> writingsByDate = writings.stream()
                .collect(Collectors.groupingBy(writing -> writing.getCreatedAt().toLocalDate()));
        Map<LocalDate, Long> errorsByDate = analyses.stream()
                .collect(Collectors.groupingBy(
                        analysis -> analysis.getCompletedAt().toLocalDate(),
                        Collectors.summingLong(analysis -> analysis.confirmedCandidates().size())));

        List<DailyTrend> trends = new ArrayList<>();
        for (LocalDate date = week.start(); week.contains(date); date = date.plusDays(1)) {
            List<Writing> ofDay = writingsByDate.getOrDefault(date, List.of());
            trends.add(new DailyTrend(
                    date,
                    ofDay.size(),
                    ofDay.stream().mapToInt(writing -> countSentences(writing.getFinalText())).sum(),
                    errorsByDate.getOrDefault(date, 0L),
                    countSelfCorrections(ofDay)));
        }
        return trends;
    }

    /**
     * 가장 많이 반복된 유형 하나만 고른다. 명세가 전문적인 진단을 금지하므로 문구는 만들지 않고
     * 유형과 근거 코드만 내려준다.
     */
    private FocusArea toFocusArea(List<ErrorProfile> profiles) {
        return profiles.stream()
                .max(Comparator.comparingInt(ErrorProfile::getOccurrenceCount))
                .map(profile -> new FocusArea(
                        profile.getErrorType(),
                        profile.getErrorType().getLabel(),
                        FocusReason.MOST_REPEATED,
                        profile.getOccurrenceCount()))
                .orElse(null);
    }

    private int countSentences(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(text.split("[.!?]"))
                .filter(sentence -> !sentence.isBlank())
                .count();
    }

    private Integer toAge(Integer birthYear) {
        return birthYear == null ? null : Year.now().getValue() - birthYear;
    }
}
