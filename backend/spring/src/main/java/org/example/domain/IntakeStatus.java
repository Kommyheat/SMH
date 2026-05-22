package org.example.domain;

//약 한 번의 복용 상태
//예를 들어 "오늘 이 시간에 약을 먹었는가?"에 대한 enum
public enum IntakeStatus {

    SCHEDULED, // 복용예정
    TAKEN,     // 복용 완료
    MISSED,     // 미복용
    DELETED   // 삭제됨
}
