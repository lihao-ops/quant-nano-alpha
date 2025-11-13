package com.xxl.job.admin.service.impl;

/**
 * 类说明 / Class Description:
 * 中文：登录服务，基于Cookie与数据库完成鉴权、会话保持与登出处理。
 * English: Login service implementing authentication, session persistence and logout via Cookie and database.
 *
 * 使用场景 / Use Cases:
 * 中文：管理端用户登录与鉴权；支持“记住我”提升使用体验。
 * English: Admin user login and authentication; supports "remember me" to improve UX.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过对称的token生成与解析避免明文暴露，结合二次校验防止伪造凭证。
 * English: Avoid plaintext exposure via symmetric token generation/parsing and prevent forged credentials via secondary validation.
 */
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.CookieUtil;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigInteger;

/**
 * 登录服务提供器，通过 Cookie + 数据库的方式完成鉴权。
 *
 * <p>设计思路：
 * <ol>
 *     <li>登录时根据数据库校验用户名密码，生成包含用户信息的十六进制 token。</li>
 *     <li>通过 {@link CookieUtil} 将 token 写入响应，配合“记住我”实现持久化登录。</li>
 *     <li>后续请求通过解析 cookie 中的 token，二次校验数据库口令，确保凭证未被篡改。</li>
 *     <li>退出登录时清除 cookie，以此作为会话失效的标志。</li>
 * </ol>
 * 核心逻辑在于保持 token 生成与校验的对称性，杜绝明文敏感信息泄露。
 */
@Service
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    @Resource
    private XxlJobUserDao xxlJobUserDao;


    // ---------------------- token tool ----------------------

    /**
     * 方法说明 / Method Description:
     * 中文：将用户对象序列化并转为16进制token，避免明文暴露。
     * English: Serialize user object and convert to hex token to avoid plaintext exposure.
     *
     * 参数 / Parameters:
     * @param xxlJobUser 中文：用户对象 / English: user object
     *
     * 返回值 / Return:
     * 中文：十六进制token字符串 / English: hexadecimal token string
     *
     * 异常 / Exceptions:
     * 中文：无 / English: none
     */
    private String makeToken(XxlJobUser xxlJobUser){
        // 将用户信息序列化后转成16进制串，避免直接暴露明文JSON
        String tokenJson = JacksonUtil.writeValueAsString(xxlJobUser);
        String tokenHex = new BigInteger(tokenJson.getBytes()).toString(16);
        return tokenHex;
    }
    /**
     * 方法说明 / Method Description:
     * 中文：解析16进制token为用户对象，供后续校验使用。
     * English: Parse hexadecimal token into user object for subsequent validation.
     *
     * 参数 / Parameters:
     * @param tokenHex 中文：十六进制token / English: hex token
     *
     * 返回值 / Return:
     * 中文：用户对象（解析失败返回null） / English: user object (null on failure)
     *
     * 异常 / Exceptions:
     * 中文：可能抛出解析异常，由调用方处理 / English: may throw parsing exceptions handled by caller
     */
    private XxlJobUser parseToken(String tokenHex){
        XxlJobUser xxlJobUser = null;
        if (tokenHex != null) {
            // 与 makeToken 保持对称解码，还原用户对象供后续校验
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());      // username_password(md5)
            xxlJobUser = JacksonUtil.readValue(tokenJson, XxlJobUser.class);
        }
        return xxlJobUser;
    }


    // ---------------------- login tool, with cookie and db ----------------------

    /**
     * 方法说明 / Method Description:
     * 中文：执行登录流程，校验用户名密码正确后下发会话token至Cookie。
     * English: Perform login; validate username/password then issue session token into Cookie.
     *
     * 参数 / Parameters:
     * @param request 中文：HTTP请求 / English: HTTP request
     * @param response 中文：HTTP响应 / English: HTTP response
     * @param username 中文：用户名 / English: username
     * @param password 中文：密码明文 / English: password plaintext
     * @param ifRemember 中文：是否记住我 / English: remember-me flag
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（登录结果） / English: ReturnT<String> login result
     *
     * 异常 / Exceptions:
     * 中文：参数为空或认证失败返回错误码与提示 / English: empty params or auth failure returns error code and message
     */
    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember){

        // param
        if (username==null || username.trim().length()==0 || password==null || password.trim().length()==0){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }

        // valid passowrd
        XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
        if (xxlJobUser == null) {
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(xxlJobUser.getPassword())) {
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }

        String loginToken = makeToken(xxlJobUser);

        // do login
        // 写入 cookie 作为会话令牌，并根据 ifRemember 控制有效期
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @param response
     */
    /**
     * 方法说明 / Method Description:
     * 中文：退出登录，清除Cookie中的会话token。
     * English: Logout by removing session token from Cookie.
     *
     * 参数 / Parameters:
     * @param request 中文：HTTP请求 / English: HTTP request
     * @param response 中文：HTTP响应 / English: HTTP response
     *
     * 返回值 / Return:
     * 中文：ReturnT<String>（操作结果） / English: ReturnT<String> operation result
     *
     * 异常 / Exceptions:
     * 中文：无 / English: none
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @return
     */
    /**
     * 方法说明 / Method Description:
     * 中文：判断当前请求是否已登录，解析Cookie并二次校验数据库口令哈希。
     * English: Determine whether current request is logged in; parse Cookie and validate DB password hash.
     *
     * 参数 / Parameters:
     * @param request 中文：HTTP请求 / English: HTTP request
     * @param response 中文：HTTP响应 / English: HTTP response
     *
     * 返回值 / Return:
     * 中文：已登录返回用户对象；否则返回null / English: user object if logged in; otherwise null
     *
     * 异常 / Exceptions:
     * 中文：解析异常将触发登出以清理状态 / English: parsing exceptions trigger logout to cleanup state
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response){
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            XxlJobUser cookieUser = null;
            try {
                // 解析 cookie，若数据损坏会触发异常，需主动登出清理状态
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                // 再次查询数据库校验密码哈希，避免伪造凭证绕过认证
                XxlJobUser dbUser = xxlJobUserDao.loadByUserName(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }


}
