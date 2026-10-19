package com.wooyeon.table.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    EVENT_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "회차를 찾을 수 없습니다"),
    EVENT_SLOT_ALREADY_EXISTS(HttpStatus.CONFLICT, "그 날짜에 이미 회차가 있습니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 상태에서는 처리할 수 없습니다"),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청을 찾을 수 없습니다"),
    PROFILE_INCOMPLETE(HttpStatus.BAD_REQUEST, "신청 전에 프로필(관심사·성향·희망 성향·식사 예산)을 채워 주세요"),
    APPLICATION_CLOSED(HttpStatus.CONFLICT, "신청이 마감된 회차입니다"),
    DUPLICATE_APPLICATION(HttpStatus.CONFLICT, "이미 신청한 날짜가 있습니다"),
    DINING_TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "테이블을 찾을 수 없습니다"),
    IMPORT_FAILED(HttpStatus.BAD_REQUEST, "신청자 파일을 읽을 수 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String message;
}
