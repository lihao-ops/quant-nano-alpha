package com.xxl.job.admin.core.route;

/**
 * 类说明 / Class Description:
 * 中文：执行器路由抽象基类，定义基于路由策略在多个执行器地址中选择目标地址的能力。
 * English: Abstract base for executor routing; defines capability to select a target address among multiple executors based on strategy.
 *
 * 使用场景 / Use Cases:
 * 中文：在触发任务时，根据路由策略挑选具体执行节点（如随机、轮询、LRU等）。
 * English: When triggering jobs, select concrete executor node per routing strategy (random, round-robin, LRU, etc.).
 *
 * 设计目的 / Design Purpose:
 * 中文：通过策略模式解耦路由算法与触发流程，便于扩展与维护。
 * English: Decouple routing algorithms from trigger flow via strategy pattern, enabling extensibility and maintainability.
 */
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by xuxueli on 17/3/10.
 */
public abstract class ExecutorRouter {
    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    /**
     * 方法说明 / Method Description:
     * 中文：在给定地址列表中选择一个目标执行地址并返回。
     * English: Select a target executor address from the given list and return it.
     *
     * 参数 / Parameters:
     * @param triggerParam 中文：触发参数（任务ID、参数、超时等） / English: trigger parameters (job ID, params, timeout, etc.)
     * @param addressList 中文：可选执行器地址列表 / English: candidate executor address list
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（content为选定的执行地址） / English: ReturnT<String> (content is selected address)
     *
     * 异常 / Exceptions:
     * 中文：实现可能在列表为空时返回失败或抛异常 / English: implementations may fail or throw when list is empty
     */
    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);

}
