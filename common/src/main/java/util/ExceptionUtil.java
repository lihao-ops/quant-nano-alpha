package util;

/**
 * 异常处理工具类
 *
 * 职责：提供异常信息格式化能力，便于日志与告警统一输出。
 *
 * 设计目的：
 * 1. 统一异常字符串格式，避免各处拼接风格不一致。
 * 2. 输出可读且可解析的异常摘要，支持快速定位问题。
 *
 * 为什么需要该类：
 * - 业务日志需要稳定的异常格式，提升排查效率。
 *
 * 核心实现思路：
 * - 提取异常类型、消息与关键堆栈片段，按无空格格式拼接。
 *
 * @author hli
 * @description: 异常处理工具类
 * @date 2020-12-17 14:13:33
 */
public class ExceptionUtil {
    private static final int MAX_FRAME_COUNT = 10;

    /**
     * 获取异常堆栈摘要字符串
     *
     * 实现逻辑：
     * 1. 读取异常类型与消息。
     * 2. 追加有限数量的堆栈片段。
     * 3. 处理cause链路并拼接结果。
     *
     * @param throwable 异常对象
     * @return 异常摘要字符串
     */
    public static String getStackTrace(Throwable throwable) {
        // 实现思路：
        // 1. 输出异常类型与消息。
        // 2. 追加关键堆栈片段与cause链路。
        if (throwable == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(256);
        appendThrowable(builder, "root", throwable);
        Throwable cause = throwable.getCause();
        int depth = 0;
        while (cause != null && cause != throwable && depth < 5) {
            appendThrowable(builder, "cause" + depth, cause);
            throwable = cause;
            cause = cause.getCause();
            depth++;
        }
        return builder.toString();
    }

    private static void appendThrowable(StringBuilder builder, String label, Throwable throwable) {
        builder.append("error_")
                .append(label)
                .append("_type=")
                .append(throwable.getClass().getName());
        String message = normalizeMessage(throwable.getMessage());
        if (!message.isEmpty()) {
            builder.append("_message=").append(message);
        }
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int limit = Math.min(stackTrace.length, MAX_FRAME_COUNT);
        for (int i = 0; i < limit; i++) {
            StackTraceElement frame = stackTrace[i];
            builder.append("|frame=")
                    .append(frame.getClassName())
                    .append(".")
                    .append(frame.getMethodName())
                    .append(":")
                    .append(frame.getFileName())
                    .append(":")
                    .append(frame.getLineNumber());
        }
    }

    private static String normalizeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.replace(" ", "_")
                .replace("\r", "_")
                .replace("\n", "_");
    }
}
