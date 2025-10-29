package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.thread.JobCompleteHelper;
import com.xxl.job.admin.core.thread.JobRegistryHelper;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端Biz接口的默认实现，主要负责接收调度执行器回调、注册和下线请求，
 * 并将这些请求委托给底层的 Helper 组件去处理。整体思路是维持一个轻量级的
 * 门面层，避免在接口实现中堆砌具体逻辑，从而保持业务层的单一职责。
 *
 * <p>核心方法仅做参数透传，强调通过单例 Helper 保证全局一致的注册表状态：</p>
 * <ul>
 *     <li>{@link #callback(List)} 将任务执行结果批量提交给 JobCompleteHelper，触发日志写入与后续调度。</li>
 *     <li>{@link #registry(RegistryParam)} 调用 JobRegistryHelper 维持执行器注册心跳。</li>
 *     <li>{@link #registryRemove(RegistryParam)} 通知 JobRegistryHelper 移除离线执行器。</li>
 * </ul>
 */
@Service
public class AdminBizImpl implements AdminBiz {


    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        // 直接将回调任务交给 JobCompleteHelper 统一处理，保证执行记录和调度链条的完整性
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        // 注册或续约执行器，通过单例 Helper 维护分布式执行器的在线状态
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        // 执行器下线时调用，确保注册中心及时清理节点信息
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }

}
