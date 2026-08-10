package com.example.writegrow.domain.activity.repository;

import com.example.writegrow.domain.activity.entity.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 이벤트는 현재 기록 전용이다.
 * 조회는 REQ-06(보호자 리포트)·REQ-07(교사 대시보드)에서 필요한 형태가 정해질 때 추가한다.
 * 그때는 기간·프로필 기준으로 계속 쌓이는 데이터이므로 페이지네이션이 필요하다.
 */
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
}
