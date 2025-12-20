package com.hao.datacollector.study.jvm.classLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类加载器演示用测试类
 *
 * 职责：提供一个可被自定义类加载器加载的简单目标类。
 *
 * 设计目的：
 * 1. 验证类加载器是否能够正常加载并执行目标类方法。
 * 2. 提供最小可观测输出，便于对加载行为做断言。
 *
 * 为什么需要该类：
 * - 类加载器实验需要一个可控的被加载类作为样本。
 *
 * 核心实现思路：
 * - 暴露简单的hello方法并输出日志。
 */
public class MyTestClass {
    private static final Logger LOG = LoggerFactory.getLogger(MyTestClass.class);

    /**
     * 打印类加载验证信息
     *
     * 实现逻辑：
     * 1. 输出加载成功的提示日志。
     * 2. 便于类加载器测试断言调用链路。
     */
    public void hello() {
        // 实现思路：
        // 1. 记录被加载类的调用痕迹。
        LOG.info("类加载验证输出|Class_loader_verify_output,message={}", "Hello_from_MyTestClass");
    }
}
