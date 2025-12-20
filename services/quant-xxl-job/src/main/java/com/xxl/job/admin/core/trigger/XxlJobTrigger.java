package com.xxl.job.admin.core.trigger;

/**
 * 类说明 / Class Description:
 * 中文：调度触发器，负责根据任务与分组配置选择执行器地址、记录触发日志并远程调用执行器。
 * English: Scheduler trigger that selects executor address per job/group config, records trigger logs, and invokes remote executor.
 *
 * 使用场景 / Use Cases:
 * 中文：在调度周期或手工触发时，统一进行路由、参数装配与执行器调用。
 * English: Used during scheduled cycles or manual triggers to perform routing, parameter assembly, and executor invocation.
 *
 * 设计目的 / Design Purpose:
 * 中文：解耦触发流程与路由/日志/远程调用细节，提升可维护性与可观测性。
 * English: Decouple trigger flow from routing/logging/remote-call details to improve maintainability and observability.
 */
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * xxl-job trigger
 * Created by xuxueli on 17/7/13.
 */
public class XxlJobTrigger {
    private static Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);

    /**
     * trigger job
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam
     * @param executorParam
     *          null: use job param
     *          not null: cover job param
     * @param addressList
     *          null: use executor addressList
     *          not null: cover
     */
    /**
     * 方法说明 / Method Description:
     * 中文：触发任务执行，支持广播分片与地址覆盖，记录触发日志并返回执行结果摘要。
     * English: Trigger job execution with sharding broadcast and address override support; record trigger log and return execution summary.
     *
     * 参数 / Parameters:
     * @param jobId 中文：任务ID / English: job ID
     * @param triggerType 中文：触发类型（如手工/调度/重试） / English: trigger type (manual/schedule/retry)
     * @param failRetryCount 中文：失败重试次数（>=0覆盖配置，<0使用任务配置） / English: fail retry count (>=0 override, <0 use job config)
     * @param executorShardingParam 中文：分片参数（index/total） / English: sharding param (index/total)
     * @param executorParam 中文：执行参数（为空则沿用任务配置） / English: executor param (null uses job config)
     * @param addressList 中文：地址列表（为空则用注册地址） / English: address list (null uses registry list)
     *
     * 返回值 / Return:
     * 中文：无（结果写入日志并由上层感知） / English: void (results written to logs and observed upstream)
     *
     * 异常 / Exceptions:
     * 中文：内部处理异常通过日志记录并安全返回 / English: internal exceptions logged and handled safely
     */
    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {

        // load data
        // 中文：加载任务与分组信息，安全覆盖执行参数与重试次数
        // English: Load job/group info; safely override exec params and retry count
        XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);
        if (jobInfo == null) {
            logger.warn("日志记录|Log_message,>>>>>>>>>>>>_trigger_fail,_jobId_invalid，jobId={}", jobId);
            return;
        }
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        int finalFailRetryCount = failRetryCount>=0?failRetryCount:jobInfo.getExecutorFailRetryCount();
        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(jobInfo.getJobGroup());

        // cover addressList
        // 中文：如指定地址列表则覆盖分组的注册信息
        // English: Override registry addresses with specified list when provided
        if (addressList!=null && addressList.trim().length()>0) {
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }

        // sharding param
        // 中文：解析分片参数为 index/total，用于广播策略
        // English: Parse sharding param into index/total for broadcast strategy
        int[] shardingParam = null;
        if (executorShardingParam!=null){
            String[] shardingArr = executorShardingParam.split("/");
            if (shardingArr.length==2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                shardingParam = new int[2];
                shardingParam[0] = Integer.valueOf(shardingArr[0]);
                shardingParam[1] = Integer.valueOf(shardingArr[1]);
            }
        }
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST==ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
                && group.getRegistryList()!=null && !group.getRegistryList().isEmpty()
                && shardingParam==null) {
            // 中文：广播分片下针对每个注册地址依次触发
            // English: Under sharding broadcast, trigger sequentially for each registry address
            for (int i = 0; i < group.getRegistryList().size(); i++) {
                processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryList().size());
            }
        } else {
            if (shardingParam == null) {
                shardingParam = new int[]{0, 1};
            }
            // 中文：非广播时按给定分片触发
            // English: Trigger by given sharding when not broadcasting
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1]);
        }

    }

    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param group                     job group, registry list may be empty
     * @param jobInfo
     * @param finalFailRetryCount
     * @param triggerType
     * @param index                     sharding index
     * @param total                     sharding index
     */
    /**
     * 方法说明 / Method Description:
     * 中文：构建触发参数、选择执行地址并调用执行器，最终记录触发日志。
     * English: Build trigger params, select executor address and invoke executor, then record trigger logs.
     *
     * 参数 / Parameters:
     * @param group 中文：任务分组（可能无注册列表） / English: job group (registry list may be empty)
     * @param jobInfo 中文：任务信息 / English: job info
     * @param finalFailRetryCount 中文：失败重试次数 / English: final fail retry count
     * @param triggerType 中文：触发类型 / English: trigger type
     * @param index 中文：分片索引 / English: sharding index
     * @param total 中文：分片总数 / English: sharding total
     *
     * 返回值 / Return:
     * 中文：无（日志记录触发行为与结果） / English: void (logs capture trigger behavior and result)
     *
     * 异常 / Exceptions:
     * 中文：远程执行异常被捕获并写入日志信息 / English: remote execution exceptions are caught and written to logs
     */
    private static void processTrigger(XxlJobGroup group, XxlJobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total){

        // param
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);  // block strategy
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);    // route strategy
        String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST==executorRouteStrategyEnum)?String.valueOf(index).concat("/").concat(String.valueOf(total)):null;

        // 1、save log-id
        // 中文：创建触发日志记录以追踪本次任务执行
        // English: Create trigger log record to trace this job execution
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        jobLog.setTriggerTime(new Date());
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().save(jobLog);
        logger.debug("日志记录|Log_message,>>>>>>>>>>>_xxl-job_trigger_start,_jobId:{}", jobLog.getId());

        // 2、init trigger-param
        // 中文：组装触发参数以传递至执行器
        // English: Assemble trigger parameters to pass to executor
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(jobLog.getId());
        triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);

        // 3、init address
        String address = null;
        ReturnT<String> routeAddressResult = null;
        if (group.getRegistryList()!=null && !group.getRegistryList().isEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                if (index < group.getRegistryList().size()) {
                    address = group.getRegistryList().get(index);
                } else {
                    address = group.getRegistryList().get(0);
                }
            } else {
                routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, group.getRegistryList());
                if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
                    address = routeAddressResult.getContent();
                }
            }
        } else {
            routeAddressResult = new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobconf_trigger_address_empty"));
        }

        // 4、trigger remote executor
        ReturnT<String> triggerResult = null;
        if (address != null) {
            triggerResult = runExecutor(triggerParam, address);
        } else {
            triggerResult = new ReturnT<String>(ReturnT.FAIL_CODE, null);
        }

        // 5、collection trigger info
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：").append(IpUtil.getIp());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：")
                .append( (group.getAddressType() == 0)?I18nUtil.getString("jobgroup_field_addressType_0"):I18nUtil.getString("jobgroup_field_addressType_1") );
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：").append(group.getRegistryList());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：").append(executorRouteStrategyEnum.getTitle());
        if (shardingParam != null) {
            triggerMsgSb.append("("+shardingParam+")");
        }
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount);

        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_run") +"<<<<<<<<<<< </span><br>")
                .append((routeAddressResult!=null&&routeAddressResult.getMsg()!=null)?routeAddressResult.getMsg()+"<br><br>":"").append(triggerResult.getMsg()!=null?triggerResult.getMsg():"");

        // 6、save log trigger-info
        // 中文：写入地址、处理器、参数与分片等关键触发信息
        // English: Write key trigger information like address, handler, params and sharding
        jobLog.setExecutorAddress(address);
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setExecutorShardingParam(shardingParam);
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);
        //jobLog.setTriggerTime();
        jobLog.setTriggerCode(triggerResult.getCode());
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(jobLog);

        logger.debug("日志记录|Log_message,>>>>>>>>>>>_xxl-job_trigger_end,_jobId:{}", jobLog.getId());
    }

    /**
     * run executor
     * @param triggerParam
     * @param address
     * @return
     */
    /**
     * 方法说明 / Method Description:
     * 中文：远程调用执行器运行任务，并返回执行结果；异常转换为字符串信息。
     * English: Invoke remote executor to run job and return result; convert exceptions to string messages.
     *
     * 参数 / Parameters:
     * @param triggerParam 中文：触发参数 / English: trigger parameters
     * @param address 中文：执行器地址 / English: executor address
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（包含代码与消息） / English: ReturnT<String> with code and message
     *
     * 异常 / Exceptions:
     * 中文：网络或执行异常被捕获并封装为失败结果 / English: network or execution exceptions are caught and wrapped as failure result
     */
    public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address){
        ReturnT<String> runResult = null;
        try {
            // 中文：根据地址获取执行器代理并发起远程执行
            // English: Get executor proxy by address and start remote execution
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error("日志记录|Log_message,>>>>>>>>>>>_xxl-job_trigger_error,_please_check_if_the_executor[{}]_is_running.", address, e);
            runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
        }

        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());

        // 中文：将规范化的结果文本写回消息字段
        // English: Write normalized result text back to message field
        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

}
