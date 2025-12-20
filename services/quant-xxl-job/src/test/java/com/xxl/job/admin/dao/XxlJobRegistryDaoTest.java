package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobRegistry;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 注册表DAO测试
 *
 * 测试目的：
 * 1. 验证注册表的保存、查询与清理逻辑。
 * 2. 验证并发场景下的注册写入行为。
 *
 * 设计思路：
 * - 通过随机端口启动上下文并执行DAO操作。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class XxlJobRegistryDaoTest {
    private static final Logger LOG = LoggerFactory.getLogger(XxlJobRegistryDaoTest.class);

    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    /**
     * 基础注册表写入与清理测试
     *
     * 实现逻辑：
     * 1. 写入注册记录并查询。
     * 2. 执行过期清理验证流程。
     */
    @Test
    public void test() {
        // 实现思路：验证DAO基本功能链路
        int ret = xxlJobRegistryDao.registrySaveOrUpdate("g1", "k1", "v1", new Date());
        /*int ret = xxlJobRegistryDao.registryUpdate("g1", "k1", "v1", new Date());
        if (ret < 1) {
            ret = xxlJobRegistryDao.registrySave("g1", "k1", "v1", new Date());
        }*/

        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(1, new Date());

        int ret2 = xxlJobRegistryDao.removeDead(Arrays.asList(1));
    }

    /**
     * 并发注册写入测试
     *
     * 实现逻辑：
     * 1. 多线程并发写入注册记录。
     * 2. 输出返回值用于观察执行结果。
     *
     * @throws InterruptedException 中断异常
     */
    @Test
    public void test2() throws InterruptedException {
        // 实现思路：多线程并发模拟注册写入
        for (int i = 0; i < 100; i++) {
            new Thread(()->{
                int ret = xxlJobRegistryDao.registrySaveOrUpdate("g1", "k1", "v1", new Date());
                LOG.info("注册写入结果|Registry_write_result,result={}", ret);

                /*int ret = xxlJobRegistryDao.registryUpdate("g1", "k1", "v1", new Date());
                if (ret < 1) {
                    ret = xxlJobRegistryDao.registrySave("g1", "k1", "v1", new Date());
                }*/
            }).start();
        }

        TimeUnit.SECONDS.sleep(10);
    }

}
