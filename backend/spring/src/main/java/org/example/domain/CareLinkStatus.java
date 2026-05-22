package org.example.domain;

// DB care_links.status ENUM과 일치
// PENDING, REJECTED 추가
// PENDING    → 연동 요청 후 상대방 수락 대기 중
// ACTIVE     → 수락 완료, 연동 중
// REJECTED   → 거절됨
// DISCONNECTED → 연동 해제
public enum CareLinkStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    DISCONNECTED
}
