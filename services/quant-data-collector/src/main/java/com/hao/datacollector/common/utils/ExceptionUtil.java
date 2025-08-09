package com.hao.datacollector.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author hli
 * @description: 异常处理类
 * @date 2020-12-17 14:13:33
 */
public class ExceptionUtil {
    /**
     * 获取详细的异常堆栈
     *
     * @param throwable
     * @return
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        try {
            throwable.printStackTrace(pw);
            return stringWriter.toString();
        } finally {
            pw.close();
        }
    }
}