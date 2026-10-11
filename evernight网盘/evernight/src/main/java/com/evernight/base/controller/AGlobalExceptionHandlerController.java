package com.evernight.base.controller;

import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理控制器
 * 统一处理系统中未捕获的异常，并返回格式化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class AGlobalExceptionHandlerController extends ABaseController {

    /**
     * 统一异常处理方法
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应对象
     */
    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request) {
        log.error("请求错误，请求地址{},错误信息:", request.getRequestURL(), e);
        ResponseVO ajaxResponse = new ResponseVO();

        //404
        if (e instanceof NoHandlerFoundException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_404.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_404.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof BusinessException) {
            //业务错误
            BusinessException biz = (BusinessException) e;
            ajaxResponse.setCode(biz.getCode() == null ? ResponseCodeEnum.CODE_400.getCode() : biz.getCode());
            ajaxResponse.setInfo(biz.getMessage());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof BindException || e instanceof MethodArgumentTypeMismatchException) {
            //参数类型错误
            ajaxResponse.setCode(ResponseCodeEnum.CODE_400.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_400.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof DuplicateKeyException) {
            //主键冲突
            ajaxResponse.setCode(ResponseCodeEnum.CODE_601.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_601.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else {
            //系统内部错误
            ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_500.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        }
        return ajaxResponse;
    }
}
