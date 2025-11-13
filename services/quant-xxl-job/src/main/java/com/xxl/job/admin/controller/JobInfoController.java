package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {
	private static Logger logger = LoggerFactory.getLogger(JobInfoController.class);

	@Resource
	private XxlJobGroupDao xxlJobGroupDao;
	@Resource
	private XxlJobService xxlJobService;
	
	@RequestMapping
	public String index(HttpServletRequest request, Model model, @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup) {

		// 枚举-字典
		// 中文：加载路由/Glue/阻塞策略/调度类型等枚举供前端渲染
		// English: Load enums for routing/glue/block strategy/schedule type for frontend rendering
		model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());	    // 路由策略-列表
		model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());								// Glue类型-字典
		model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());	    // 阻塞处理策略-字典
		model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());	    				// 调度类型
		model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());	    			// 调度过期策略

		// 执行器列表
		List<XxlJobGroup> jobGroupList_all =  xxlJobGroupDao.findAll();

		// filter group
		List<XxlJobGroup> jobGroupList = PermissionInterceptor.filterJobGroupByRole(request, jobGroupList_all);
		if (jobGroupList==null || jobGroupList.size()==0) {
			throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
		}

		model.addAttribute("JobGroupList", jobGroupList);
		model.addAttribute("jobGroup", jobGroup);

		return "jobinfo/jobinfo.index";
	}

	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(@RequestParam(value = "start", required = false, defaultValue = "0") int start,
										@RequestParam(value = "length", required = false, defaultValue = "10") int length,
										@RequestParam("jobGroup") int jobGroup,
										@RequestParam("triggerStatus") int triggerStatus,
										@RequestParam("jobDesc") String jobDesc,
										@RequestParam("executorHandler") String executorHandler,
										@RequestParam("author") String author) {
		
		return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
	}
	
	@RequestMapping("/add")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：新增任务接口，先校验分组权限再委托服务层持久化。
	 * English: API to add job; validate group permission then delegate to service layer.
	 *
	 * 参数 / Parameters:
	 * @param request 中文：HTTP请求 / English: HTTP request
	 * @param jobInfo 中文：任务信息 / English: job info
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（结果与提示） / English: ReturnT<String> result and message
	 *
	 * 异常 / Exceptions:
	 * 中文：权限不足或参数非法时失败 / English: failure on insufficient permission or invalid params
	 */
	public ReturnT<String> add(HttpServletRequest request, XxlJobInfo jobInfo) {
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

		// opt
		XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
		return xxlJobService.add(jobInfo, loginUser);
	}
	
	@RequestMapping("/update")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：更新任务接口，校验权限后委托服务层执行变更。
	 * English: API to update job; validate permission then delegate to service layer.
	 *
	 * 参数 / Parameters:
	 * @param request 中文：HTTP请求 / English: HTTP request
	 * @param jobInfo 中文：任务信息 / English: job info
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
	 *
	 * 异常 / Exceptions:
	 * 中文：权限不足或参数非法时失败 / English: failure on insufficient permission or invalid params
	 */
	public ReturnT<String> update(HttpServletRequest request, XxlJobInfo jobInfo) {
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

		// opt
		XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
		return xxlJobService.update(jobInfo, loginUser);
	}
	
	@RequestMapping("/remove")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：删除任务接口。
	 * English: API to remove job.
	 *
	 * 参数 / Parameters:
	 * @param id 中文：任务ID / English: job ID
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
	 *
	 * 异常 / Exceptions:
	 * 中文：数据访问异常由服务层抛出 / English: data access exceptions propagated from service layer
	 */
	public ReturnT<String> remove(@RequestParam("id") int id) {
		return xxlJobService.remove(id);
	}
	
	@RequestMapping("/stop")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：停止任务接口。
	 * English: API to stop job.
	 *
	 * 参数 / Parameters:
	 * @param id 中文：任务ID / English: job ID
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
	 *
	 * 异常 / Exceptions:
	 * 中文：数据访问异常由服务层抛出 / English: data access exceptions propagated from service layer
	 */
	public ReturnT<String> pause(@RequestParam("id") int id) {
		return xxlJobService.stop(id);
	}
	
	@RequestMapping("/start")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：启动任务接口。
	 * English: API to start job.
	 *
	 * 参数 / Parameters:
	 * @param id 中文：任务ID / English: job ID
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
	 *
	 * 异常 / Exceptions:
	 * 中文：配置非法或数据访问异常由服务层抛出 / English: invalid config or data access exceptions from service layer
	 */
	public ReturnT<String> start(@RequestParam("id") int id) {
		return xxlJobService.start(id);
	}
	
	@RequestMapping("/trigger")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：手工触发任务接口，支持传入执行参数与地址列表。
	 * English: API to manually trigger job with executor params and address list.
	 *
	 * 参数 / Parameters:
	 * @param request 中文：HTTP请求 / English: HTTP request
	 * @param id 中文：任务ID / English: job ID
	 * @param executorParam 中文：执行参数 / English: executor parameter
	 * @param addressList 中文：执行器地址列表 / English: executor address list
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（触发结果） / English: ReturnT<String> trigger result
	 *
	 * 异常 / Exceptions:
	 * 中文：权限不足、任务不存在或路由异常 / English: insufficient permissions, job not found or routing errors
	 */
	public ReturnT<String> triggerJob(HttpServletRequest request,
									  @RequestParam("id") int id,
									  @RequestParam("executorParam") String executorParam,
									  @RequestParam("addressList") String addressList) {

		// login user
		XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
		// trigger
		return xxlJobService.trigger(loginUser, id, executorParam, addressList);
	}

	@RequestMapping("/nextTriggerTime")
	@ResponseBody
	/**
	 * 方法说明 / Method Description:
	 * 中文：计算未来五次触发时间的预览接口。
	 * English: Preview API to compute next five trigger times.
	 *
	 * 参数 / Parameters:
	 * @param scheduleType 中文：调度类型 / English: schedule type
	 * @param scheduleConf 中文：调度配置 / English: schedule configuration
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<List<String>>（未来触发时间字符串列表） / English: ReturnT<List<String>> of next trigger times as strings
	 *
	 * 异常 / Exceptions:
	 * 中文：配置非法或计算异常时返回失败 / English: failure on invalid config or computing errors
	 */
	public ReturnT<List<String>> nextTriggerTime(@RequestParam("scheduleType") String scheduleType,
												 @RequestParam("scheduleConf") String scheduleConf) {

		XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
		paramXxlJobInfo.setScheduleType(scheduleType);
		paramXxlJobInfo.setScheduleConf(scheduleConf);

		List<String> result = new ArrayList<>();
		try {
			Date lastTime = new Date();
			for (int i = 0; i < 5; i++) {
				lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
				if (lastTime != null) {
					result.add(DateUtil.formatDateTime(lastTime));
				} else {
					break;
				}
			}
		} catch (Exception e) {
			logger.error("nextTriggerTime error. scheduleType = {}, scheduleConf= {}", scheduleType, scheduleConf, e);
			return new ReturnT<List<String>>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) + e.getMessage());
		}
		return new ReturnT<List<String>>(result);

	}
	
}
