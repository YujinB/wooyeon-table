package com.wooyeon.table.global.exception;

import com.wooyeon.table.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), e.getMessage()));
    }

    /** @Valid 검증 실패: 첫 번째 필드 오류를 알려준다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(MethodArgumentNotValidException e) {
        ErrorCode code = ErrorCode.INVALID_INPUT;
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(code.getMessage());
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), detail));
    }

    /** JSON 형식 오류·알 수 없는 enum 값 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        ErrorCode code = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), code.getMessage()));
    }

    /** 유일 제약 위반 = 같은 요청이 동시에 두 번 들어온 경우 (예: 해결 방식 중복 선택) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DataIntegrityViolationException e) {
        log.warn("유일 제약 위반: {}", e.getMostSpecificCause().getMessage());
        ErrorCode code = ErrorCode.DUPLICATE_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), code.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), code.getMessage()));
    }
}
