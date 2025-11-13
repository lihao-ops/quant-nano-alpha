package com.xxl.job.admin.service;

/**
 * 类说明 / Class Description:
 * 中文：任务调度核心服务接口，定义作业的分页查询、增删改、启停、触发以及仪表盘与图表数据的查询。
 * English: Core job scheduling service interface; defines job pagination, CRUD, start/stop, trigger operations, and dashboard/chart metric queries.
 *
 * 使用场景 / Use Cases:
 * 中文：供控制器与管理端调用以统一管理调度中心任务生命周期与监控数据。
 * English: Used by controllers and admin to uniformly manage job lifecycle and monitoring data in the scheduling center.
 *
 * 设计目的 / Design Purpose:
 * 中文：抽象出任务调度的领域能力，隔离实现细节，提升可维护性与可测试性。
 * English: Abstract domain capabilities of job scheduling, isolate implementation details, and improve maintainability and testability.
 */

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * core job action for xxl-job
 * 
 * @author xuxueli 2016-5-28 15:30:33
 */
public interface XxlJobService {

    /**
     * 方法说明 / Method Description:
     * 中文：分页查询调度任务，支持按分组、状态与关键字段过滤。
     * English: Paginate job list with filtering by group, status and key fields.
     *
     * 参数 / Parameters:
     * @param start 中文：起始偏移 / English: pagination start offset
     * @param length 中文：每页长度 / English: page size
     * @param jobGroup 中文：任务分组ID / English: job group ID
     * @param triggerStatus 中文：触发状态过滤 / English: trigger status filter
     * @param jobDesc 中文：任务描述关键词 / English: job description keyword
     * @param executorHandler 中文：执行器处理器名称 / English: executor handler name
     * @param author 中文：负责人名称 / English: author/owner name
     *
     * 返回值 / Return:
     * 中文：分页数据Map（包含总数与列表） / English: map containing total count and list data
     *
     * 异常 / Exceptions:
     * 中文：实现可能抛出数据访问异常 / English: implementation may throw data access exceptions
     */
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);

    /**
     * 方法说明 / Method Description:
     * 中文：新增调度任务并进行参数校验与持久化。
     * English: Add a new scheduled job with parameter validation and persistence.
     *
     * 参数 / Parameters:
     * @param jobInfo 中文：任务信息实体 / English: job info entity
     * @param loginUser 中文：登录用户，用于审计与权限校验 / English: login user for audit and permission check
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（包含操作结果与提示信息） / English: ReturnT<String> containing operation result and message
     *
     * 异常 / Exceptions:
     * 中文：可能抛出参数校验或数据访问异常 / English: may throw validation or data access exceptions
     */
    public ReturnT<String> add(XxlJobInfo jobInfo, XxlJobUser loginUser);

    /**
     * 方法说明 / Method Description:
     * 中文：更新调度任务配置，确保版本一致性与安全校验。
     * English: Update scheduled job configuration ensuring version consistency and security checks.
     *
     * 参数 / Parameters:
     * @param jobInfo 中文：任务信息实体 / English: job info entity
     * @param loginUser 中文：登录用户，用于审计与权限校验 / English: login user for audit and permission check
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
     *
     * 异常 / Exceptions:
     * 中文：可能抛出并发更新、参数校验或数据访问异常 / English: may throw concurrent update, validation or data access exceptions
     */
    public ReturnT<String> update(XxlJobInfo jobInfo, XxlJobUser loginUser);

    /**
     * 方法说明 / Method Description:
     * 中文：删除调度任务并清理关联数据，如日志与注册信息。
     * English: Remove scheduled job and cleanup associated data such as logs and registrations.
     *
     * 参数 / Parameters:
     * @param id 中文：任务ID / English: job ID
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
     *
     * 异常 / Exceptions:
     * 中文：可能抛出数据访问异常 / English: may throw data access exceptions
     */
    public ReturnT<String> remove(int id);

    /**
     * 方法说明 / Method Description:
     * 中文：启动（激活）调度任务，使其按配置参与调度触发。
     * English: Start (activate) a scheduled job to participate in scheduling triggers per configuration.
     *
     * 参数 / Parameters:
     * @param id 中文：任务ID / English: job ID
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
     *
     * 异常 / Exceptions:
     * 中文：可能抛出数据访问或状态校验异常 / English: may throw data access or state validation exceptions
     */
    public ReturnT<String> start(int id);

    /**
     * 方法说明 / Method Description:
     * 中文：停止（禁用）调度任务，阻止进一步触发。
     * English: Stop (disable) a scheduled job to prevent further triggers.
     *
     * 参数 / Parameters:
     * @param id 中文：任务ID / English: job ID
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
     *
     * 异常 / Exceptions:
     * 中文：可能抛出数据访问或状态校验异常 / English: may throw data access or state validation exceptions
     */
    public ReturnT<String> stop(int id);

    /**
     * 方法说明 / Method Description:
     * 中文：手动触发一次任务执行，可携带运行参数与指定地址列表。
     * English: Manually trigger a job execution with runtime parameters and optional address list.
     *
     * 参数 / Parameters:
     * @param loginUser 中文：操作用户，用于审计与权限校验 / English: operator user for audit and permission check
     * @param jobId 中文：任务ID / English: job ID
     * @param executorParam 中文：执行参数字符串 / English: executor param string
     * @param addressList 中文：指定执行器地址列表（可选） / English: specified executor addresses (optional)
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（触发结果与消息） / English: ReturnT<String> trigger result and message
     *
     * 异常 / Exceptions:
     * 中文：可能抛出路由、网络或权限相关异常 / English: may throw routing, network or permission related exceptions
     */
    public ReturnT<String> trigger(XxlJobUser loginUser, int jobId, String executorParam, String addressList);

    /**
     * 方法说明 / Method Description:
     * 中文：查询仪表盘汇总信息（任务数、执行器数、日志统计等）。
     * English: Query dashboard summary info such as job counts, executor counts, and log stats.
     *
     * 参数 / Parameters:
     * @return 中文：统计信息Map / English: summary info map
     *
     * 异常 / Exceptions:
     * 中文：实现可能抛出数据访问异常 / English: implementation may throw data access exceptions
     */
    public Map<String,Object> dashboardInfo();

    /**
     * 方法说明 / Method Description:
     * 中文：查询指定时间范围内的图表统计数据，用于趋势分析与监控。
     * English: Query chart statistics within a time range for trend analysis and monitoring.
     *
     * 参数 / Parameters:
     * @param startDate 中文：起始日期 / English: start date
     * @param endDate 中文：结束日期 / English: end date
     *
     * 返回值 / Return:
     * 中文：ReturnT<Map<String,Object>>（图表数据） / English: ReturnT<Map<String,Object>> chart data
     *
     * 异常 / Exceptions:
     * 中文：实现可能抛出数据访问异常 / English: implementation may throw data access exceptions
     */
    public ReturnT<Map<String,Object>> chartInfo(Date startDate, Date endDate);

}
