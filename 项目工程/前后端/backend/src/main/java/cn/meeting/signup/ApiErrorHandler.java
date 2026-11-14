package cn.meeting.signup;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import static cn.meeting.signup.Models.*;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger log=LoggerFactory.getLogger(ApiErrorHandler.class);
    private ResponseEntity<ApiError> error(int status,String code,String message,HttpServletRequest request){
        return ResponseEntity.status(status).body(new ApiError(code,message,String.valueOf(request.getAttribute("requestId"))));
    }
    @ExceptionHandler(ApiException.class) ResponseEntity<ApiError> business(ApiException e,HttpServletRequest r){return error(e.status,e.code,e.getMessage(),r);}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e,HttpServletRequest r){
        var field=e.getBindingResult().getFieldError();
        String message=field==null?"请检查填写内容":switch(field.getField()){
            case "username" -> "账号须为3至32位字母、数字或下划线，以字母开头";
            case "displayName" -> "请填写称呼，最多32个字符";
            case "password" -> e.getTarget() instanceof CreateAccountRequest?"密码须为8至64个字符":"请填写密码，最多64个字符";
            default -> "请检查填写内容";
        };
        return error(400,"VALIDATION_ERROR",message,r);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> malformed(Exception e,HttpServletRequest r){return error(400,"VALIDATION_ERROR","请检查JSON正文和ID格式",r);}
    @ExceptionHandler(DuplicateKeyException.class) ResponseEntity<ApiError> duplicate(Exception e,HttpServletRequest r){return error(409,"ALREADY_REGISTERED","你已经报名了这场活动",r);}
    @ExceptionHandler(DataAccessException.class) ResponseEntity<ApiError> database(Exception e,HttpServletRequest r){
        log.warn("requestId={} database failure {}",r.getAttribute("requestId"),e.getClass().getSimpleName());
        return error(503,"DATABASE_NOT_READY","数据库访问失败，请检查连接、库名及建表步骤",r);
    }
    @ExceptionHandler(NoResourceFoundException.class) ResponseEntity<ApiError> notFound(Exception e,HttpServletRequest r){return error(404,"NOT_FOUND","接口或页面不存在",r);}
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class) ResponseEntity<ApiError> media(Exception e,HttpServletRequest r){return error(415,"UNSUPPORTED_MEDIA_TYPE","请求正文请选择application/json",r);}
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class) ResponseEntity<ApiError> method(Exception e,HttpServletRequest r){return error(405,"METHOD_NOT_ALLOWED","请求方法不支持",r);}
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> unknown(Exception e,HttpServletRequest r){
        log.error("requestId={} unexpected failure",r.getAttribute("requestId"),e);
        return error(500,"INTERNAL_ERROR","服务暂时无法处理请求，请查看对应请求日志",r);
    }
}
