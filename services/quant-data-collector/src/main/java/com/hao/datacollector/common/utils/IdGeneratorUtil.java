package com.hao.datacollector.common.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ID生成工具类
 * 提供静态方法生成分布式唯一ID
 *
 * <p>实现思路:</p>
 * <p>该工具类作为SnowflakeIdGenerator的静态访问入口，解决在Spring环境中通过静态方法调用非静态Bean的问题。</p>
 * <p>通过Spring的@Autowired注入SnowflakeIdGenerator实例，并在@PostConstruct方法中将其赋值给静态变量instance。</p>
 * <p>所有对ID生成的需求都通过静态方法委托给instance持henius的SnowflakeIdGenerator实例。</p>
 *
 * <p>实现步骤:</p>
 * <p>1. 定义一个静态变量instance用于持有IdGeneratorUtil的单例。</p>
 * <p>2. 使用@Autowired注入SnowflakeIdGenerator的实例。</p>
 * <p>3. 在使用@PostConstruct注解的init方法中，将当前实例(this)赋值给静态变量instance，并将注入的snowflakeIdGenerator赋值给instance的对应字段。</p>
 * <p>4. 提供静态方法nextId()和nextIdStr()，内部通过instance调用snowflakeIdGenerator的相应方法。</p>
 */
// 将此类标记为Spring组件，使其可以被Spring容器管理和注入
@Component
// 定义IdGeneratorUtil类
public class IdGeneratorUtil {

    // 自动注入SnowflakeIdGenerator实例
    @Autowired
    // 声明一个私有的SnowflakeIdGenerator类型的成员变量，用于持有注入的ID生成器实例
    private SnowflakeIdGenerator snowflakeIdGenerator;

    // 静态变量，用于持有IdGeneratorUtil的单例
    // 声明一个私有的静态IdGeneratorUtil类型的成员变量，用于持有类的单例实例
    private static IdGeneratorUtil instance;

    /**
     * 初始化方法，在依赖注入完成后执行
     * 将当前实例赋值给静态变量instance，并设置其snowflakeIdGenerator字段
     *
     * <p>实现思路:</p>
     * <p>利用@PostConstruct注解，确保在Spring完成依赖注入后执行此方法。</p>
     * <p>将Spring管理的IdGeneratorUtil实例赋值给静态变量instance，从而使得静态方法可以访问到注入的snowflakeIdGenerator。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 使用@PostConstruct注解标记此方法。</p>
     * <p>2. 将当前对象(this)赋值给静态变量instance。</p>
     * <p>3. 将通过@Autowired注入的snowflakeIdGenerator赋值给instance的snowflakeIdGenerator字段。</p>
     */
    // 使用@PostConstruct注解标记此方法，确保在依赖注入完成后执行
    @PostConstruct
    // 定义初始化方法init
    public void init() {
        // 将当前实例(this)赋值给静态变量instance，实现单例模式的静态访问
        instance = this;
        // 将通过@Autowired注入的snowflakeIdGenerator赋值给静态instance的相应字段
        instance.snowflakeIdGenerator = this.snowflakeIdGenerator;
    }

    /**
     * 获取下一个雪花算法生成的ID
     * 通过静态单例调用SnowflakeIdGenerator的nextId方法
     *
     * @return 分布式唯一ID
     *
     * <p>实现思路:</p>
     * <p>提供一个静态方法作为获取ID的统一入口。</p>
     * <p>内部调用静态instance持有的SnowflakeIdGenerator实例的nextId方法。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 定义一个公共静态方法nextId()。</p>
     * <p>2. 返回instance.snowflakeIdGenerator.nextId()的调用结果。</p>
     */
    // 定义一个公共静态方法nextId，用于获取下一个ID
    public static long nextId() {
        // 通过静态instance调用其持有的snowflakeIdGenerator的nextId方法，并返回结果
        return instance.snowflakeIdGenerator.nextId();
    }

    /**
     * 获取下一个雪花算法生成的ID (字符串形式)
     * 将生成的Long类型ID转换为字符串
     *
     * @return 分布式唯一ID字符串
     *
     * <p>实现思路:</p>
     * <p>提供一个静态方法用于获取字符串形式的ID。</p>
     * <p>内部调用nextId()方法获取Long类型ID，然后将其转换为字符串。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 定义一个公共静态方法nextIdStr()。</p>
     * <p>2. 调用nextId()方法获取Long类型ID。</p>
     * <p>3. 使用String.valueOf()将Long类型ID转换为字符串并返回。</p>
     */
    // 定义一个公共静态方法nextIdStr，用于获取字符串形式的ID
    public static String nextIdStr() {
        // 调用nextId()方法获取Long类型ID，并转换为字符串返回
        return String.valueOf(nextId());
    }
}