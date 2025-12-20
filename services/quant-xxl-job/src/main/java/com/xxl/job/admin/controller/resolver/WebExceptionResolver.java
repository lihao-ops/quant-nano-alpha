package com.xxl.job.admin.controller.resolver;

/**
 * 类说明 / Class Description:
 * 中文：Web全局异常解析器，统一将控制层异常转换为JSON或错误视图响应。
 * English: Global web exception resolver; converts controller exceptions into JSON or error view responses uniformly.
 *
 * 使用场景 / Use Cases:
 * 中文：当控制器方法抛出异常时拦截并输出标准化错误结构，以提升前后端协作效率。
 * English: Intercepts controller exceptions to output standardized error structures for efficient frontend-backend collaboration.
 *
 * 设计目的 / Design Purpose:
 * 中文：避免异常信息泄露与页面混乱，通过注解识别响应类型，提升系统健壮性与可观测性。
 * English: Prevent info leakage and messy pages; identify response type via annotations to enhance robustness and observability.
 */
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.admin.core.util.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * common exception resolver
 *
 * @author xuxueli 2016-1-6 19:22:18
 */
@Component
public class WebExceptionResolver implements HandlerExceptionResolver {
	private static transient Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class);

	@Override
	/**
	 * 方法说明 / Method Description:
	 * 中文：解析控制器异常，根据是否包含@ResponseBody注解决定返回JSON或视图。
	 * English: Resolve controller exceptions; return JSON or view based on presence of @ResponseBody.
	 *
	 * 参数 / Parameters:
	 * @param request 中文：HTTP请求 / English: HTTP request
	 * @param response 中文：HTTP响应 / English: HTTP response
	 * @param handler 中文：处理器对象 / English: handler object
	 * @param ex 中文：捕获的异常 / English: captured exception
	 *
	 * 返回值 / Return:
	 * 中文：ModelAndView（JSON响应时为空视图；页面响应时为错误视图） / English: ModelAndView (empty view for JSON; error view for page)
	 *
	 * 异常 / Exceptions:
	 * 中文：IO异常在写入响应时记录日志 / English: IO exceptions during response writing are logged
	 */
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		if (!(ex instanceof XxlJobException)) {
			logger.error("日志记录|Log_message,WebExceptionResolver:{}", ex);
		}

		// if json
		boolean isJson = false;
		// 中文：识别处理方法是否声明为JSON响应
		// English: Detect whether the handler method declares JSON response
		if (handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
			if (responseBody != null) {
				isJson = true;
			}
		}

		// error result
		// 中文：构造标准错误结果（换行替换为HTML换行）
		// English: Build standardized error result (replace newlines with HTML breaks)
		ReturnT<String> errorResult = new ReturnT<String>(ReturnT.FAIL_CODE, ex.toString().replaceAll("\n", "<br/>"));

		// response
		ModelAndView mv = new ModelAndView();
		if (isJson) {
			try {
				// 中文：写出统一JSON错误响应
				// English: Write unified JSON error response
				response.setContentType("application/json;charset=utf-8");
				response.getWriter().print(JacksonUtil.writeValueAsString(errorResult));
			} catch (IOException e) {
				logger.error("日志记录|Log_message,exception={}", e.getMessage(), e);
			}
			return mv;
		} else {
		
			// 中文：设置错误消息并返回统一错误视图
			// English: Set error message and return unified error view
			mv.addObject("exceptionMsg", errorResult.getMsg());
			mv.setViewName("/common/common.exception");
			return mv;
		}
	}
	
}
