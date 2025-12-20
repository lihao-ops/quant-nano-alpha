package com.xxl.job.admin.util;

import com.xxl.job.admin.core.util.I18nUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 国际化工具测试
 *
 * 设计目的：
 * 1. 验证基础国际化键值读取是否正常。
 * 2. 验证多键合并文案输出是否符合预期。
 *
 * 实现思路：
 * - 分别调用单键与多键读取接口并输出结果。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class I18nUtilTest {
    private static Logger logger = LoggerFactory.getLogger(I18nUtilTest.class);

    /**
     * 验证国际化工具输出
     *
     * 实现逻辑：
     * 1. 读取单键文案。
     * 2. 读取多键合并文案。
     * 3. 输出默认多键文案。
     */
    @Test
    public void test() {
        logger.info("日志记录|Log_message,i18n_admin_name={}", I18nUtil.getString("admin_name"));
        logger.info("日志记录|Log_message,i18n_admin_name_full={}", I18nUtil.getMultString("admin_name", "admin_name_full"));
        logger.info("日志记录|Log_message,i18n_mult_string={}", I18nUtil.getMultString());
    }

}
