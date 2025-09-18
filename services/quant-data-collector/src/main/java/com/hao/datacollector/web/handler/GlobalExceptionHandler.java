package com.hao.datacollector.web.handler;

import com.hao.datacollector.web.vo.result.ResultVO;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-09-18 21:39:40
 * @description: 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResultVO<String> handleException(Exception e) {
        ResultVO<String> resultVO = new ResultVO<>();
        resultVO.setCode(500);
        resultVO.setMessage(e.getMessage());
        resultVO.setData("系统繁忙!");
        return resultVO;
    }
}
