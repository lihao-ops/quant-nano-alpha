package com.xxl.job.admin.controller;

import com.xxl.job.admin.service.impl.LoginService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * JobInfoController 接口测试
 *
 * 设计目的：
 * 1. 验证登录获取 Cookie 的流程是否正常。
 * 2. 验证分页查询接口返回结果可用。
 *
 * 实现思路：
 * - BeforeEach 登录并缓存 Cookie。
 * - 通过 pageList 接口验证接口返回内容。
 */
public class JobInfoControllerTest extends AbstractSpringMvcTest {
  private static Logger logger = LoggerFactory.getLogger(JobInfoControllerTest.class);

  private Cookie cookie;

  /**
   * 登录并获取认证 Cookie
   *
   * 实现逻辑：
   * 1. 调用登录接口。
   * 2. 从响应中提取登录 Cookie。
   */
  @BeforeEach
  public void login() throws Exception {
    MvcResult ret = mockMvc.perform(
        post("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("userName", "admin")
            .param("password", "123456")
    ).andReturn();
    cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
  }

  /**
   * 验证分页查询接口响应
   *
   * 实现逻辑：
   * 1. 构造分页查询参数。
   * 2. 调用接口并记录返回内容。
   */
  @Test
  public void testAdd() throws Exception {
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
    parameters.add("jobGroup", "1");
    parameters.add("triggerStatus", "-1");

    MvcResult ret = mockMvc.perform(
        post("/jobinfo/pageList")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            //.content(paramsJson)
            .params(parameters)
            .cookie(cookie)
    ).andReturn();

    logger.info("日志记录|Log_message,response={}", ret.getResponse().getContentAsString());
  }

}
