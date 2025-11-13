package com.xxl.job.executorbiz;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证执行器客户端接口的可用性，包括心跳、空闲检测、任务运行、终止与日志查询。
 * English: Verify executor client API availability including beat, idle check, run, kill, and log queries.
 *
 * 预期结果 / Expected Result:
 * 中文：各接口返回结构不为空，状态码符合预期；运行接口在空环境下返回非空结果。
 * English: Each API returns non-null structures with expected status codes; run API returns non-null result in empty environment.
 *
 * 执行方式 / How to Execute:
 * 中文：在开发/测试环境运行单元测试，确保 addressUrl 指向可访问的执行器或模拟端点。
 * English: Run unit tests in dev/test environment; ensure addressUrl points to reachable executor or mock endpoint.
 */
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * executor api test
 *
 * Created by xuxueli on 17/5/12.
 */
public class ExecutorBizTest {

    // admin-client
    private static String addressUrl = "http://127.0.0.1:9999/";
    private static String accessToken = null;
    private static int timeout = 3;

    @Test
    /**
     * 测试说明 / What is tested:
     * 中文：测试执行器心跳接口的连通性。
     * English: Test executor beat API connectivity.
     *
     * 输入/输出 / Input/Output:
     * 中文：输入为无参调用；输出为 ReturnT<String> 心跳结果。
     * English: Input is parameterless call; output is ReturnT<String> beat result.
     *
     * 预期结果 / Expected:
     * 中文：返回码为200，消息为空，content为空。
     * English: Code 200, msg null, content null.
     */
    public void beat() throws Exception {
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);
        // Act
        final ReturnT<String> retval = executorBiz.beat();

        // Assert result
        Assertions.assertNotNull(retval);
        Assertions.assertNull(((ReturnT<String>) retval).getContent());
        Assertions.assertEquals(200, retval.getCode());
        Assertions.assertNull(retval.getMsg());
    }

    @Test
    /**
     * 测试说明 / What is tested:
     * 中文：测试空闲检测接口，当线程运行或队列存在时返回失败码。
     * English: Test idle beat API; returns failure code when thread running or trigger queue exists.
     *
     * 输入/输出 / Input/Output:
     * 中文：输入为 jobId；输出为 ReturnT<String>。
     * English: Input jobId; output ReturnT<String>.
     *
     * 预期结果 / Expected:
     * 中文：返回码为500，消息为线程占用或队列存在提示。
     * English: Code 500 with message indicating running or queue present.
     */
    public void idleBeat(){
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        final int jobId = 0;

        // Act
        final ReturnT<String> retval = executorBiz.idleBeat(new IdleBeatParam(jobId));

        // Assert result
        Assertions.assertNotNull(retval);
        Assertions.assertNull(((ReturnT<String>) retval).getContent());
        Assertions.assertEquals(500, retval.getCode());
        Assertions.assertEquals("job thread is running or has trigger queue.", retval.getMsg());
    }

    @Test
    /**
     * 测试说明 / What is tested:
     * 中文：测试执行器运行接口，在参数完整时可返回非空结果。
     * English: Test executor run API; returns non-null result when params are complete.
     *
     * 输入/输出 / Input/Output:
     * 中文：输入为 TriggerParam；输出为 ReturnT<String>。
     * English: Input TriggerParam; output ReturnT<String>.
     *
     * 预期结果 / Expected:
     * 中文：返回结构非空。
     * English: Non-null return structure.
     */
    public void run(){
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        // trigger data
        final TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);
        triggerParam.setExecutorHandler("demoJobHandler");
        triggerParam.setExecutorParams(null);
        triggerParam.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.COVER_EARLY.name());
        triggerParam.setGlueType(GlueTypeEnum.BEAN.name());
        triggerParam.setGlueSource(null);
        triggerParam.setGlueUpdatetime(System.currentTimeMillis());
        triggerParam.setLogId(1);
        triggerParam.setLogDateTime(System.currentTimeMillis());

        // Act
        final ReturnT<String> retval = executorBiz.run(triggerParam);

        // Assert result
        Assertions.assertNotNull(retval);
    }

    @Test
    /**
     * 测试说明 / What is tested:
     * 中文：测试终止接口，验证返回码与消息字段。
     * English: Test kill API; verify return code and message fields.
     *
     * 输入/输出 / Input/Output:
     * 中文：输入为 jobId；输出为 ReturnT<String>。
     * English: Input jobId; output ReturnT<String>.
     *
     * 预期结果 / Expected:
     * 中文：返回码为200，content与msg为空。
     * English: Code 200; content and msg null.
     */
    public void kill(){
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        final int jobId = 0;

        // Act
        final ReturnT<String> retval = executorBiz.kill(new KillParam(jobId));

        // Assert result
        Assertions.assertNotNull(retval);
        Assertions.assertNull(((ReturnT<String>) retval).getContent());
        Assertions.assertEquals(200, retval.getCode());
        Assertions.assertNull(retval.getMsg());
    }

    @Test
    /**
     * 测试说明 / What is tested:
     * 中文：测试日志查询接口的返回结构。
     * English: Test log API return structure.
     *
     * 输入/输出 / Input/Output:
     * 中文：输入为 LogParam；输出为 ReturnT<LogResult>。
     * English: Input LogParam; output ReturnT<LogResult>.
     *
     * 预期结果 / Expected:
     * 中文：返回结构非空。
     * English: Non-null return structure.
     */
    public void log(){
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        final long logDateTim = 0L;
        final long logId = 0;
        final int fromLineNum = 0;

        // Act
        final ReturnT<LogResult> retval = executorBiz.log(new LogParam(logDateTim, logId, fromLineNum));

        // Assert result
        Assertions.assertNotNull(retval);
    }

}
