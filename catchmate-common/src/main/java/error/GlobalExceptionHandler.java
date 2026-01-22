package error; // 패키지 위치는 메인 모듈 스캔 범위 내에 있어야 함

import error.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler { // extends 제거

    // 1. 커스텀 비즈니스 에러
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    // 2. @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class) // 직접 지정
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {

        // 에러 메시지 추출 로직
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();

        return ErrorResponse.toResponseEntity(ErrorCode.BAD_REQUEST, message);
    }

    // 3. JSON 파싱 실패 (요청 바디 형식 오류)
    @ExceptionHandler(HttpMessageNotReadableException.class) // 직접 지정
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {

        log.warn("Invalid request body: {}", e.getMessage());

        return ErrorResponse.toResponseEntity(
                ErrorCode.BAD_REQUEST,
                "요청 포맷이 올바르지 않습니다. (JSON 형식을 확인해주세요)"
        );
    }

    // 4. 나머지 서버 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Server Error: ", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
