package org.example.domain;

//해당 날짜의 전체 복약 상태
public enum CalenderDayStatus {
    EMPTY,       // 복약 일정 없음
    SCHEDULED,  // 예정 있을 때, 아직 복용 전
    COMPLETED,  // 해당 날짜 복약 전부 완료
    PARTIAL,    // 일부만 완료되었을 때
    MISSED      // 지난 날짜인데 미복용 있음
}
