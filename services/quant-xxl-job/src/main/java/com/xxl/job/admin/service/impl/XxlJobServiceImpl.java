package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.cron.CronExpression;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLogReport;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.*;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;

/**
 * XXL-JOB 调度中心的核心实现，围绕任务的增删改查与触发进行封装。
 * <p>
 * 主要流程包括：校验请求参数 → 组装调度配置 → 调用 DAO 持久化 →
 * 结合 {@link JobScheduleHelper}、{@link JobTriggerPoolHelper} 驱动执行节点。
 * </p>
 *
 * core job action for xxl-job
 * @author xuxueli 2016-5-28 15:30:33
 */
@Service
public class XxlJobServiceImpl implements XxlJobService {
	private static Logger logger = LoggerFactory.getLogger(XxlJobServiceImpl.class);

	@Resource
	private XxlJobGroupDao xxlJobGroupDao;
	@Resource
	private XxlJobInfoDao xxlJobInfoDao;
	@Resource
	public XxlJobLogDao xxlJobLogDao;
	@Resource
	private XxlJobLogGlueDao xxlJobLogGlueDao;
	@Resource
	private XxlJobLogReportDao xxlJobLogReportDao;
	
	@Override
	/**
	 * 方法说明 / Method Description:
	 * 中文：分页查询任务列表并返回数据与总数，支持多条件过滤。
	 * English: Query paginated job list with counts, supporting multi-criteria filters.
	 *
	 * 参数 / Parameters:
	 * @param start 中文：起始偏移 / English: start offset
	 * @param length 中文：每页长度 / English: page length
	 * @param jobGroup 中文：任务分组ID / English: job group ID
	 * @param triggerStatus 中文：触发状态过滤 / English: trigger status filter
	 * @param jobDesc 中文：任务描述关键词 / English: job description keyword
	 * @param executorHandler 中文：执行器处理器名称 / English: executor handler name
	 * @param author 中文：负责人名称 / English: author name
	 *
	 * 返回值 / Return:
	 * 中文：包含总数与当前页数据的 Map / English: Map with total and page data
	 *
	 * 异常 / Exceptions:
	 * 中文：数据访问异常由 DAO 层抛出 / English: data access exceptions may be thrown by DAO
	 */
	public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {

		// page list
		// 中文：通过DAO按条件分页查询任务列表
		// English: Use DAO to query job list with filters and pagination
		List<XxlJobInfo> list = xxlJobInfoDao.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
		// 中文：计算过滤后的总记录数以支持前端分页展示
		// English: Compute filtered total record count for frontend pagination
		int list_count = xxlJobInfoDao.pageListCount(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
		
		// package result
		// 中文：封装列表与统计为统一响应结构
		// English: Package list and stats into a unified response map
		Map<String, Object> maps = new HashMap<String, Object>();
	    maps.put("recordsTotal", list_count);		// 总记录数
	    maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
	    maps.put("data", list);  					// 分页列表
		return maps;
	}

	@Override
	/**
	 * 方法说明 / Method Description:
	 * 中文：新增调度任务，执行分组、描述、负责人与触发配置等综合校验后持久化。
	 * English: Add a scheduled job after validating group, description, owner and trigger configurations, then persist.
	 *
	 * 参数 / Parameters:
	 * @param jobInfo 中文：任务信息实体 / English: job info entity
	 * @param loginUser 中文：当前登录用户，用于权限校验 / English: current login user for permission validation
	 *
	 * 返回值 / Return:
	 * 中文：ReturnT<String>（成功返回ID，失败返回错误信息） / English: ReturnT<String> (ID on success, error message on failure)
	 *
	 * 异常 / Exceptions:
	 * 中文：参数异常、调度配置异常与数据持久化异常 / English: parameter errors, scheduling config errors, and persistence errors
	 */
	public ReturnT<String> add(XxlJobInfo jobInfo, XxlJobUser loginUser) {

		// valid base
		// 中文：加载任务分组以进行存在性校验
		// English: Load job group to validate existence
		XxlJobGroup group = xxlJobGroupDao.load(jobInfo.getJobGroup());
		if (group == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose")+I18nUtil.getString("jobinfo_field_jobgroup")) );
		}
		if (jobInfo.getJobDesc()==null || jobInfo.getJobDesc().trim().length()==0) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_jobdesc")) );
		}
		if (jobInfo.getAuthor()==null || jobInfo.getAuthor().trim().length()==0) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_author")) );
		}

		// valid trigger
		// 中文：匹配调度类型，保障类型合法
		// English: Match schedule type to ensure legality
		ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
		if (scheduleTypeEnum == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
		}
		if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
			if (jobInfo.getScheduleConf()==null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, "Cron"+I18nUtil.getString("system_unvalid"));
			}
		} else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE/* || scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
			if (jobInfo.getScheduleConf() == null) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")) );
			}
			try {
				// 中文：解析固定频率秒数
				// English: Parse fixed rate seconds
				int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
				if (fixSecond < 1) {
					return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
				}
			} catch (Exception e) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
			}
		}

		// valid job
		// 中文：校验GLUE类型合法性
		// English: Validate GLUE type legality
		if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype")+I18nUtil.getString("system_unvalid")) );
		}
		if (GlueTypeEnum.BEAN==GlueTypeEnum.match(jobInfo.getGlueType()) && (jobInfo.getExecutorHandler()==null || jobInfo.getExecutorHandler().trim().length()==0) ) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+"JobHandler") );
		}
		// 》fix "\r" in shell
		if (GlueTypeEnum.GLUE_SHELL==GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource()!=null) {
			jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
		}

		// valid advanced
		if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy")+I18nUtil.getString("system_unvalid")) );
		}
		if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy")+I18nUtil.getString("system_unvalid")) );
		}
		if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy")+I18nUtil.getString("system_unvalid")) );
		}

		// 》ChildJobId valid
		if (jobInfo.getChildJobId()!=null && jobInfo.getChildJobId().trim().length()>0) {
			String[] childJobIds = jobInfo.getChildJobId().split(",");
			for (String childJobIdItem: childJobIds) {
				if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
					XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
					if (childJobInfo==null) {
						return new ReturnT<String>(ReturnT.FAIL_CODE,
								MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_not_found")), childJobIdItem));
					}
					if (!loginUser.validPermission(childJobInfo.getJobGroup())) {
						return new ReturnT<String>(ReturnT.FAIL_CODE,
								MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_permission_limit")), childJobIdItem));
					}
				} else {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_unvalid")), childJobIdItem));
				}
			}

			// join , avoid "xxx,,"
			String temp = "";
			for (String item:childJobIds) {
				temp += item + ",";
			}
			temp = temp.substring(0, temp.length()-1);

			jobInfo.setChildJobId(temp);
		}

		// add in db
		// 中文：设置创建与更新时间戳
		// English: Set create and update timestamps
		jobInfo.setAddTime(new Date());
		jobInfo.setUpdateTime(new Date());
		jobInfo.setGlueUpdatetime(new Date());
		xxlJobInfoDao.save(jobInfo);
		if (jobInfo.getId() < 1) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add")+I18nUtil.getString("system_fail")) );
		}

		return new ReturnT<String>(String.valueOf(jobInfo.getId()));
	}

	/**
	 * 方法说明 / Method Description:
	 * 中文：判断字符串是否为整数数字形式。
	 * English: Determine whether the given string is a valid integer number.
	 *
	 * 参数 / Parameters:
	 * @param str 中文：待判断字符串 / English: string to validate
	 *
	 * 返回值 / Return:
	 * 中文：true表示是整数；false表示不是 / English: true if integer; false otherwise
	 *
	 * 异常 / Exceptions:
	 * 中文：无；内部捕获NumberFormatException / English: none; NumberFormatException caught internally
	 */
	private boolean isNumeric(String str){
		try {
			int result = Integer.valueOf(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public ReturnT<String> update(XxlJobInfo jobInfo, XxlJobUser loginUser) {

		// valid base
		if (jobInfo.getJobDesc()==null || jobInfo.getJobDesc().trim().length()==0) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_jobdesc")) );
		}
		if (jobInfo.getAuthor()==null || jobInfo.getAuthor().trim().length()==0) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_author")) );
		}

		// valid trigger
		ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
		if (scheduleTypeEnum == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
		}
		if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
			if (jobInfo.getScheduleConf()==null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, "Cron"+I18nUtil.getString("system_unvalid") );
			}
		} else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
			if (jobInfo.getScheduleConf() == null) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
			}
			try {
				int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
				if (fixSecond < 1) {
					return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
				}
			} catch (Exception e) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
			}
		}

		// valid advanced
		if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy")+I18nUtil.getString("system_unvalid")) );
		}
		if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy")+I18nUtil.getString("system_unvalid")) );
		}
		if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy")+I18nUtil.getString("system_unvalid")) );
		}

		// 》ChildJobId valid
		if (jobInfo.getChildJobId()!=null && jobInfo.getChildJobId().trim().length()>0) {
			String[] childJobIds = jobInfo.getChildJobId().split(",");
			for (String childJobIdItem: childJobIds) {
				if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
					// parse child
					int childJobId = Integer.parseInt(childJobIdItem);
					if (childJobId == jobInfo.getId()) {
						return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_childJobId")+"("+childJobId+")"+I18nUtil.getString("system_unvalid")) );
					}

					// valid child
					XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(childJobId);
					if (childJobInfo==null) {
						return new ReturnT<String>(ReturnT.FAIL_CODE,
								MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_not_found")), childJobIdItem));
					}
					if (!loginUser.validPermission(childJobInfo.getJobGroup())) {
						return new ReturnT<String>(ReturnT.FAIL_CODE,
								MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_permission_limit")), childJobIdItem));
					}
				} else {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_unvalid")), childJobIdItem));
				}
			}

			// join , avoid "xxx,,"
			String temp = "";
			for (String item:childJobIds) {
				temp += item + ",";
			}
			temp = temp.substring(0, temp.length()-1);

			jobInfo.setChildJobId(temp);
		}

		// group valid
		XxlJobGroup jobGroup = xxlJobGroupDao.load(jobInfo.getJobGroup());
		if (jobGroup == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup")+I18nUtil.getString("system_unvalid")) );
		}

		// stage job info
		XxlJobInfo exists_jobInfo = xxlJobInfoDao.loadById(jobInfo.getId());
		if (exists_jobInfo == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id")+I18nUtil.getString("system_not_found")) );
		}

		// next trigger time (5s后生效，避开预读周期)
		long nextTriggerTime = exists_jobInfo.getTriggerNextTime();
		boolean scheduleDataNotChanged = jobInfo.getScheduleType().equals(exists_jobInfo.getScheduleType()) && jobInfo.getScheduleConf().equals(exists_jobInfo.getScheduleConf());
		if (exists_jobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
			try {
				Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
				if (nextValidTime == null) {
					return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
				}
				nextTriggerTime = nextValidTime.getTime();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
			}
		}

		exists_jobInfo.setJobGroup(jobInfo.getJobGroup());
		exists_jobInfo.setJobDesc(jobInfo.getJobDesc());
		exists_jobInfo.setAuthor(jobInfo.getAuthor());
		exists_jobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
		exists_jobInfo.setScheduleType(jobInfo.getScheduleType());
		exists_jobInfo.setScheduleConf(jobInfo.getScheduleConf());
		exists_jobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
		exists_jobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
		exists_jobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
		exists_jobInfo.setExecutorParam(jobInfo.getExecutorParam());
		exists_jobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
		exists_jobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
		exists_jobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
		exists_jobInfo.setChildJobId(jobInfo.getChildJobId());
		exists_jobInfo.setTriggerNextTime(nextTriggerTime);

		exists_jobInfo.setUpdateTime(new Date());
        xxlJobInfoDao.update(exists_jobInfo);


		return ReturnT.SUCCESS;
	}

	@Override
	public ReturnT<String> remove(int id) {
		XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
		if (xxlJobInfo == null) {
			return ReturnT.SUCCESS;
		}

		xxlJobInfoDao.delete(id);
		xxlJobLogDao.delete(id);
		xxlJobLogGlueDao.deleteByJobId(id);
		return ReturnT.SUCCESS;
	}

	@Override
	public ReturnT<String> start(int id) {
		// load and valid
		XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
		if (xxlJobInfo == null) {
			return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}

		// valid
		ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(xxlJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
		if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type_none_limit_start")) );
		}

		// next trigger time (5s后生效，避开预读周期)
		long nextTriggerTime = 0;
		try {
			Date nextValidTime = JobScheduleHelper.generateNextValidTime(xxlJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
			if (nextValidTime == null) {
				return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
			}
			nextTriggerTime = nextValidTime.getTime();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
		}

		xxlJobInfo.setTriggerStatus(1);
		xxlJobInfo.setTriggerLastTime(0);
		xxlJobInfo.setTriggerNextTime(nextTriggerTime);

		xxlJobInfo.setUpdateTime(new Date());
		xxlJobInfoDao.update(xxlJobInfo);
		return ReturnT.SUCCESS;
	}

	@Override
	public ReturnT<String> stop(int id) {
		// load and valid
        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
		if (xxlJobInfo == null) {
			return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}

		// stop
		xxlJobInfo.setTriggerStatus(0);
		xxlJobInfo.setTriggerLastTime(0);
		xxlJobInfo.setTriggerNextTime(0);

		xxlJobInfo.setUpdateTime(new Date());
		xxlJobInfoDao.update(xxlJobInfo);
		return ReturnT.SUCCESS;
	}



	@Override
	public ReturnT<String> trigger(XxlJobUser loginUser, int jobId, String executorParam, String addressList) {
		// permission
		// 中文：校验当前用户存在性与权限
		// English: Validate current user existence and permission
		if (loginUser == null) {
			return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_permission_limit"));
		}
		// 中文：加载目标任务并校验存在性
		// English: Load target job and validate existence
		XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(jobId);
		if (xxlJobInfo == null) {
			return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}
		if (!hasPermission(loginUser, xxlJobInfo.getJobGroup())) {
			return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_permission_limit"));
		}

		// force cover job param
		// 中文：空参数兼容处理，避免Null传递
		// English: Null-safe handling for executor parameters
		if (executorParam == null) {
			executorParam = "";
		}

		JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
		return ReturnT.SUCCESS;
	}

	/**
	 * 方法说明 / Method Description:
	 * 中文：校验用户是否具备目标任务分组的操作权限。
	 * English: Check whether user has operation permission on the target job group.
	 *
	 * 参数 / Parameters:
	 * @param loginUser 中文：当前用户 / English: current user
	 * @param jobGroup 中文：任务分组ID / English: job group ID
	 *
	 * 返回值 / Return:
	 * 中文：true表示有权限；false表示无权限 / English: true if permitted; false otherwise
	 *
	 * 异常 / Exceptions:
	 * 中文：无 / English: none
	 */
	private boolean hasPermission(XxlJobUser loginUser, int jobGroup){
		if (loginUser.getRole() == 1) {
			return true;
		}
		List<String> groupIdStrs = new ArrayList<>();
		if (loginUser.getPermission()!=null && loginUser.getPermission().trim().length()>0) {
			groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
		}
		return groupIdStrs.contains(String.valueOf(jobGroup));
	}

	@Override
	public Map<String, Object> dashboardInfo() {

		int jobInfoCount = xxlJobInfoDao.findAllCount();
		int jobLogCount = 0;
		int jobLogSuccessCount = 0;
		XxlJobLogReport xxlJobLogReport = xxlJobLogReportDao.queryLogReportTotal();
		if (xxlJobLogReport != null) {
			jobLogCount = xxlJobLogReport.getRunningCount() + xxlJobLogReport.getSucCount() + xxlJobLogReport.getFailCount();
			jobLogSuccessCount = xxlJobLogReport.getSucCount();
		}

		// executor count
		Set<String> executorAddressSet = new HashSet<String>();
		List<XxlJobGroup> groupList = xxlJobGroupDao.findAll();

		if (groupList!=null && !groupList.isEmpty()) {
			for (XxlJobGroup group: groupList) {
				if (group.getRegistryList()!=null && !group.getRegistryList().isEmpty()) {
					executorAddressSet.addAll(group.getRegistryList());
				}
			}
		}

		int executorCount = executorAddressSet.size();

		Map<String, Object> dashboardMap = new HashMap<String, Object>();
		dashboardMap.put("jobInfoCount", jobInfoCount);
		dashboardMap.put("jobLogCount", jobLogCount);
		dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
		dashboardMap.put("executorCount", executorCount);
		return dashboardMap;
	}

	@Override
	public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {

		// process
		List<String> triggerDayList = new ArrayList<String>();
		List<Integer> triggerDayCountRunningList = new ArrayList<Integer>();
		List<Integer> triggerDayCountSucList = new ArrayList<Integer>();
		List<Integer> triggerDayCountFailList = new ArrayList<Integer>();
		int triggerCountRunningTotal = 0;
		int triggerCountSucTotal = 0;
		int triggerCountFailTotal = 0;

		List<XxlJobLogReport> logReportList = xxlJobLogReportDao.queryLogReport(startDate, endDate);

		if (logReportList!=null && logReportList.size()>0) {
			for (XxlJobLogReport item: logReportList) {
				String day = DateUtil.formatDate(item.getTriggerDay());
				int triggerDayCountRunning = item.getRunningCount();
				int triggerDayCountSuc = item.getSucCount();
				int triggerDayCountFail = item.getFailCount();

				triggerDayList.add(day);
				triggerDayCountRunningList.add(triggerDayCountRunning);
				triggerDayCountSucList.add(triggerDayCountSuc);
				triggerDayCountFailList.add(triggerDayCountFail);

				triggerCountRunningTotal += triggerDayCountRunning;
				triggerCountSucTotal += triggerDayCountSuc;
				triggerCountFailTotal += triggerDayCountFail;
			}
		} else {
			for (int i = -6; i <= 0; i++) {
				triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(new Date(), i)));
				triggerDayCountRunningList.add(0);
				triggerDayCountSucList.add(0);
				triggerDayCountFailList.add(0);
			}
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("triggerDayList", triggerDayList);
		result.put("triggerDayCountRunningList", triggerDayCountRunningList);
		result.put("triggerDayCountSucList", triggerDayCountSucList);
		result.put("triggerDayCountFailList", triggerDayCountFailList);

		result.put("triggerCountRunningTotal", triggerCountRunningTotal);
		result.put("triggerCountSucTotal", triggerCountSucTotal);
		result.put("triggerCountFailTotal", triggerCountFailTotal);

		return new ReturnT<Map<String, Object>>(result);
	}

}
