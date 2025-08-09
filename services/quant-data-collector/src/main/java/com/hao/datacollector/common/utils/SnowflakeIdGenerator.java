package com.hao.datacollector.common.utils;

import org.springframework.stereotype.Component;

/**
 * 雪花算法分布式ID生成器
 *
 * <p>实现思路:</p>
 * <p>基于Twitter的Snowflake算法，生成64位的分布式唯一ID。</p>
 * <p>ID结构：1位符号位 + 41位时间戳 + 5位数据中心ID + 5位工作机器ID + 12位序列号。</p>
 * <p>通过控制数据中心ID和工作机器ID，保证在分布式环境下的唯一性。</p>
 * <p>通过序列号保证同一毫秒内的唯一性。</p>
 *
 * <p>ID结构详解:</p>
 * <p>总共64位，存储在一个long型整数中。</p>
 * <p>最高位（第63位）是符号位，固定为0，表示生成的ID为正数。</p>
 * <p>接下来的41位是时间戳，精确到毫秒，记录了当前时间与设定的起始时间（twepoch）的差值。</p>
 * <p>再接下来的5位是数据中心ID，用于标识不同的数据中心（最大支持32个）。</p>
 * <p>再接下来的5位是工作机器ID，用于标识同一数据中心内的不同机器（最大支持32个）。</p>
 * <p>最后12位是序列号，用于记录同一毫秒内生成的ID数量（最大支持4096个）。</p>
 *
 * <p>唯一性保证:</p>
 * <p>时间戳保证了ID的趋势递增和不同毫秒的唯一性。</p>
 * <p>数据中心ID和工作机器ID保证了在分布式环境下的唯一性。</p>
 * <p>序列号保证了同一毫秒内的唯一性。</p>
 *
 * <p>递增性保证:</p>
 * <p>由于时间戳在高位，生成的ID总体上是随着时间递增的。</p>
 * <p>同一毫秒内，序列号递增，也保证了递增性。</p>
 *
 * <p>63位范围内保证:</p>
 * <p>通过将最高位的符号位固定为0，确保生成的ID是一个正数，并且在long类型的63位正数范围内。</p>
 *
 * <p>实现步骤:</p>
 * <p>1. 定义各部分的位数和偏移量常量。</p>
 * <p>2. 定义数据中心ID、工作机器ID、序列号和上次生成ID的时间戳变量。</p>
 * <p>3. 构造函数用于初始化数据中心ID和工作机器ID，并进行合法性检查。</p>
 * <p>4. 实现nextId()方法，使用synchronized保证线程安全。</p>
 * <p>5. 在nextId()方法中，获取当前时间戳，处理时钟回退和同一毫秒内序列号生成。</p>
 * <p>6. 实现tilNextMillis()方法，用于处理同一毫秒内序列号溢出的情况，阻塞到下一个毫秒。</p>
 * <p>7. 实现timeGen()方法，用于获取当前时间戳。</p>
 * <p>8. 将各部分通过位运算组合成最终的64位ID。</p>
 * <p>9. 返回生成的ID，并通过位运算确保最高位为0（正数）。</p>
 */
@Component
public class SnowflakeIdGenerator {

    // 开始时间截 (2023-01-01 00:00:00.000)，用于计算时间差
    // 这个时间戳是固定的，作为ID生成的时间基准，可以根据实际项目上线时间进行调整，但一旦确定不能修改。
    private final long twepoch = 1672531200000L;
    // 数据中心ID所占的位数 (5位，最大支持2^5 - 1 = 31个数据中心，加上0共32个)
    private final long datacenterIdBits = 5L;
    // 工作机器ID所占的位数 (5位，最大支持2^5 - 1 = 31个工作机器，加上0共32个)
    private final long workerIdBits = 5L;
    // 支持的最大数据中心ID，结果是31 (0b11111)
    // 通过位运算计算出5位能表示的最大十进制数。
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    // 支持的最大工作机器ID，结果是31 (0b11111)
    // 通过位运算计算出5位能表示的最大十进制数。
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 序列号ID所占的位数 (12位，每毫秒内最大支持2^12 - 1 = 4095个序列号，加上0共4096个)
    private final long sequenceBits = 12L;
    // 工作机器ID向左移12位 (序列号位数)
    // 这是为了在最终的64位ID中为工作机器ID腾出位置。
    private final long workerIdShift = sequenceBits;
    // 数据中心ID向左移17位 (序列号位数 + 工作机器ID位数)
    // 这是为了在最终的64位ID中为数据中心ID腾出位置。
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    // 时间截向左移22位 (序列号位数 + 工作机器ID位数 + 数据中心ID位数)
    // 这是为了在最终的64位ID中为时间戳腾出最高位（符号位之后）的位置。
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    // 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)，用于取序列号的低12位
    // 通过与这个掩码进行位与运算，可以确保序列号不会超过12位所能表示的最大值。
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    // 数据中心ID(0~31)，运行时确定
    // 用于标识当前ID生成器所在的数据中心。
    private long datacenterId;
    // 工作机器ID(0~31)，运行时确定
    // 用于标识当前ID生成器所在的工作机器。
    private long workerId;
    // 毫秒内序列(0~4095)，同一毫秒内递增
    // 用于在同一毫秒内生成多个唯一ID。
    private long sequence = 0L;
    // 上次生成ID的时间截，用于判断是否是同一毫秒
    // 记录上一次生成ID时的时间戳，用于检测时钟回退和判断是否进入新的毫秒。
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     * 默认使用0作为数据中心ID和工作机器ID
     *
     * <p>实现思路:</p>
     * <p>提供一个无参构造函数，方便Spring等框架进行实例化。</p>
     * <p>默认使用0作为数据中心ID和工作机器ID，适用于单机或简单分布式场景。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 定义无参构造函数。</p>
     * <p>2. 调用带参构造函数，传入默认值0L, 0L。</p>
     */
    // 定义无参构造函数
    public SnowflakeIdGenerator() {
        // 调用带参构造函数，使用默认的数据中心ID和工作机器ID
        this(0L, 0L);
    }

    /**
     * 构造函数
     *
     * @param datacenterId 数据中心ID (0~31)
     * @param workerId     工作ID (0~31)
     *
     *                     <p>实现思路:</p>
     *                     <p>提供一个带参构造函数，允许外部指定数据中心ID和工作机器ID。</p>
     *                     <p>在设置ID之前，进行合法性检查，确保ID在有效范围内。</p>
     *
     *                     <p>实现步骤:</p>
     *                     <p>1. 定义带参构造函数，接收datacenterId和workerId。</p>
     *                     <p>2. 检查datacenterId是否在合法范围内，超出则抛出异常。</p>
     *                     <p>3. 检查workerId是否在合法范围内，超出则抛出异常。</p>
     *                     <p>4. 将传入的datacenterId和workerId赋值给类的成员变量。</p>
     */
    // 定义带参构造函数，接收数据中心ID和工作机器ID
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        // 检查数据中心ID是否超出最大范围或小于0
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            // 如果超出范围，抛出IllegalArgumentException异常
            throw new IllegalArgumentException(String.format("数据中心ID不能大于%d或小于0", maxDatacenterId));
        }
        // 检查工作机器ID是否超出最大范围或小于0
        if (workerId > maxWorkerId || workerId < 0) {
            // 如果超出范围，抛出IllegalArgumentException异常
            throw new IllegalArgumentException(String.format("工作机器ID不能大于%d或小于0", maxWorkerId));
        }
        // 将传入的数据中心ID赋值给成员变量
        this.datacenterId = datacenterId;
        // 将传入的工作机器ID赋值给成员变量
        this.workerId = workerId;
    }

    /**
     * 获取下一个雪花算法生成的ID (线程安全)
     *
     * @return SnowflakeId
     *
     * <p>实现思路:</p>
     * <p>这是生成ID的核心方法，使用synchronized关键字保证多线程环境下的线程安全。</p>
     * <p>根据当前时间戳和上次生成ID的时间戳，计算序列号，并结合数据中心ID、工作机器ID和时间戳生成最终ID。</p>
     * <p>处理时钟回退和同一毫秒内序列号溢出的情况。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 使用synchronized关键字修饰方法，保证线程安全。</p>
     * <p>2. 获取当前时间戳。</p>
     * <p>3. 检查当前时间戳是否小于上次生成ID的时间戳，如果是，说明时钟回退，抛出异常。</p>
     * <p>4. 如果当前时间戳等于上次生成ID的时间戳，说明在同一毫秒内，序列号递增。</p>
     * <p>5. 如果序列号溢出（达到最大值），则阻塞到下一个毫秒。</p>
     * <p>6. 如果当前时间戳大于上次生成ID的时间戳，说明进入新的毫秒，序列号归零。</p>
     * <p>7. 更新上次生成ID的时间戳为当前时间戳。</p>
     * <p>8. 使用位运算将时间戳、数据中心ID、工作机器ID和序列号组合成最终的64位ID。</p>
     * <p>9. 返回生成的ID，并通过位运算确保最高位为0（正数）。</p>
     */
    // 使用synchronized关键字修饰方法，保证线程安全
    public synchronized long nextId() {
        // 获取当前时间戳
        long timestamp = timeGen();
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，应当抛出异常
        if (timestamp < lastTimestamp) {
            // 抛出运行时异常，提示时钟回退
            throw new RuntimeException(
                    String.format("时钟向后移动。拒绝生成ID，直到%d毫秒", lastTimestamp - timestamp));
        }
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            // 序列号在当前基础上加1，并与序列号掩码进行位与运算，确保不超过最大序列号
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出 (序列号达到最大值且再次加1变为0)
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 跨毫秒，序列归零
            sequence = 0L;
        }
        // 更新最后的时间戳为当前时间戳
        lastTimestamp = timestamp;
        // 移位并通过或运算拼到一起组成64位的ID，并保证最高位为0（不超过63位）
        // (当前时间戳 - 开始时间截) 左移 时间截偏移量 (41位)
        // | (数据中心ID 左移 数据中心ID偏移量 (5位))
        // | (工作机器ID 左移 工作机器ID偏移量 (5位))
        // | 序列号 (12位)
        long id = ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
        // 确保ID不超过63位（最高位为0），即为正数
        // 通过与0x7FFFFFFFFFFFFFFFL进行位与运算，强制将最高位设置为0。
        return id & 0x7FFFFFFFFFFFFFFFL;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     *
     * <p>实现思路:</p>
     * <p>当同一毫秒内的序列号用尽时，调用此方法，循环获取当前时间戳，直到获取的时间戳大于上次生成ID的时间戳。</p>
     * <p>这 effectively 阻塞了当前线程，直到进入下一个毫秒。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 获取当前时间戳。</p>
     * <p>2. 循环检查当前时间戳是否小于或等于上次生成ID的时间戳。</p>
     * <p>3. 如果是，继续获取当前时间戳。</p>
     * <p>4. 直到获取的时间戳大于上次生成ID的时间戳，返回新的时间戳。</p>
     */
    // 阻塞到下一个毫秒，直到获得新的时间戳
    protected long tilNextMillis(long lastTimestamp) {
        // 获取当前时间戳
        long timestamp = timeGen();
        // 循环，直到当前时间戳大于上次生成ID的时间戳
        while (timestamp <= lastTimestamp) {
            // 继续获取当前时间戳
            timestamp = timeGen();
        }
        // 返回新的时间戳
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     *
     * <p>实现思路:</p>
     * <p>提供一个方法用于获取当前的系统时间戳，精确到毫秒。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 调用System.currentTimeMillis()方法获取当前时间戳。</p>
     * <p>2. 返回获取的时间戳。</p>
     */
    // 返回以毫秒为单位的当前时间
    protected long timeGen() {
        // 调用System.currentTimeMillis()方法获取当前时间戳
        return System.currentTimeMillis();
    }
}