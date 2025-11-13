package com.xxl.job.admin.core.route.strategy;

/**
 * 类说明 / Class Description:
 * 中文：轮询路由策略，根据每个任务的路由计数对地址列表进行取模，均衡选择执行器节点。
 * English: Round-robin routing strategy; uses per-job routing count modulo address list size to select executor evenly.
 *
 * 使用场景 / Use Cases:
 * 中文：需要在多个执行器之间进行均衡分配时使用。
 * English: Use when needing even distribution across multiple executors.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过本地计数与轻量缓存实现低开销的轮询路由，降低首次热启动压力。
 * English: Implement low-overhead round-robin routing via local counters and light caching, alleviating initial warm-up pressure.
 */
import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteRound extends ExecutorRouter {

    private static ConcurrentMap<Integer, AtomicInteger> routeCountEachJob = new ConcurrentHashMap<>();
    private static long CACHE_VALID_TIME = 0;

    /**
     * 方法说明 / Method Description:
     * 中文：获取并递增任务的路由计数，支持每日有效期缓存与首次随机化以降低集中命中。
     * English: Get and increment per-job routing count; supports daily cache validity and initial randomization to reduce hotspot hits.
     *
     * 参数 / Parameters:
     * @param jobId 中文：任务ID / English: job ID
     *
     * 返回值 / Return:
     * 中文：当前计数值 / English: current count value
     *
     * 异常 / Exceptions:
     * 中文：无 / English: none
     */
    private static int count(int jobId) {
        // cache clear
        // 中文：按有效期每日清理计数缓存，避免内存膨胀
        // English: Clear count cache daily by validity to avoid memory growth
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            routeCountEachJob.clear();
            // 中文：缓存有效期设为24小时（1000*60*60*24 毫秒）
            // English: Set cache validity to 24 hours (1000*60*60*24 ms)
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000*60*60*24;
        }

        AtomicInteger count = routeCountEachJob.get(jobId);
        if (count == null || count.get() > 1000000) {
            // 中文：当计数不存在或超过阈值(1000000)时，使用0-99随机初始值以降低冷启动集中命中
            // English: When count is missing or exceeds threshold (1,000,000), use random 0-99 initial value to reduce cold-start hotspot
            count = new AtomicInteger(new Random().nextInt(100));
        } else {
            // 中文：递增计数用于轮询步进
            // English: Increment count for round-robin stepping
            count.addAndGet(1);
        }
        routeCountEachJob.put(jobId, count);
        return count.get();
    }

    @Override
    /**
     * 方法说明 / Method Description:
     * 中文：根据轮询计数选择目标地址，保证不同执行器的均衡分配。
     * English: Select target address based on round-robin count to ensure balanced distribution across executors.
     *
     * 参数 / Parameters:
     * @param triggerParam 中文：触发参数（含任务ID） / English: trigger parameters (including job ID)
     * @param addressList 中文：执行器地址列表 / English: list of executor addresses
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（选定地址） / English: ReturnT<String> (selected address)
     *
     * 异常 / Exceptions:
     * 中文：当列表为空时应由上层进行保护 / English: Upper layer should guard against empty list
     */
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        // 中文：通过对地址列表长度取模实现轮询选择
        // English: Use modulo of address list size to implement round-robin selection
        String address = addressList.get(count(triggerParam.getJobId())%addressList.size());
        return new ReturnT<String>(address);
    }

}
