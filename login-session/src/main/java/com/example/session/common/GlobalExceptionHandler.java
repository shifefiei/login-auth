package com.example.session.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 全局异常处理：
 * - 继承 ResponseEntityExceptionHandler，保留框架对 404/405/400 等的原始 HTTP 语义；
 * - 统一把响应体包装成项目的 Result 格式，并记录日志；
 * - 业务异常（重复用户名）按业务码返回；未知异常兜底 500。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 数据库唯一键冲突（并发注册同名用户）：按业务码返回，HTTP 200。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("用户名唯一键冲突: {}", e.getMessage());
        return ResponseEntity.ok(Result.error(1001, "用户名已存在"));
    }

    /**
     * 兜底：未被框架和上面处理器覆盖的未知异常，记录堆栈并返回 500。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }

    /**
     * 框架异常（参数校验、404、405、报文不可读等）统一出口：
     * 保留父类计算出的状态码，包装成 Result 响应体，并按级别记录日志。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("框架异常(5xx): {}", ex.getMessage(), ex);
        } else {
            log.warn("框架异常({}): {}", statusCode.value(), ex.getMessage());
        }
        Result<Void> result = Result.error(statusCode.value(), resolveMessage(ex));
        return new ResponseEntity<>(result, headers, statusCode);
    }

    private String resolveMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manv) {
            return manv.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .orElse("参数校验失败");
        }
        return ex.getMessage() != null ? ex.getMessage() : "请求处理失败";
    }
}
