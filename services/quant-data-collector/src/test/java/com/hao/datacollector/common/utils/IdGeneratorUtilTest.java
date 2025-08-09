package com.hao.datacollector.common.utils;

// 导入JUnit 5的Test注解，用于标记测试方法

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdGeneratorUtil工具类的单元测试
 * 用于验证IdGeneratorUtil生成的ID的唯一性、递增性、结构和性能。
 *
 * <p>实现思路:</p>
 * <p>通过JUnit 5框架编写多个测试方法，分别测试IdGeneratorUtil的不同功能和特性。</p>
 * <p>使用单线程和多线程环境测试ID的唯一性。</p>
 * <p>测试ID的递增性。</p>
 * <p>测试ID的结构是否符合雪花算法的预期。</p>
 * <p>测试生成ID的性能。</p>
 *
 * <p>实现步骤:</p>
 * <p>1. 编写testIdUniqueness方法，在单线程下生成大量ID，使用Set验证唯一性，使用List验证递增性。</p>
 * <p>2. 编写testConcurrentIdUniqueness方法，使用线程池和CountDownLatch在多线程下生成大量ID，使用线程安全的Set验证唯一性。</p>
 * <p>3. 编写testIdStructure方法，生成一个ID，验证其是否为正数，并打印其二进制形式进行手动检查。</p>
 * <p>4. 编写testStringIdGeneration方法，测试nextIdStr()方法，验证生成的字符串非空且可解析为正数Long。</p>
 * <p>5. 编写testPerformance方法，测试生成大量ID的耗时，计算平均每秒生成数，并验证性能是否达标。</p>
 * <p>6. 编写testMillionIdUniqueness方法，测试生成一百万个ID的唯一性。</p>
 */
// 标记此类为Spring Boot测试类，会加载Spring应用上下文
@Nested
@SpringBootTest
// 定义IdGeneratorUtilTest测试类
class IdGeneratorUtilTest {

    /**
     * 测试单线程下生成的ID是否唯一和递增
     *
     * <p>实现思路:</p>
     * <p>在单个线程中连续生成大量ID，将ID存储到Set和List中。</p>
     * <p>通过Set的特性验证ID的唯一性（Set不允许重复元素）。</p>
     * <p>通过List中的顺序验证ID的递增性。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 定义生成ID的数量。</p>
     * <p>2. 创建一个HashSet用于存储ID，验证唯一性。</p>
     * <p>3. 创建一个ArrayList用于存储ID，验证递增性。</p>
     * <p>4. 循环生成指定数量的ID。</p>
     * <p>5. 在每次生成ID后，断言Set中不包含当前ID，然后将ID添加到Set和List中。</p>
     * <p>6. 循环结束后，断言Set的大小等于生成的ID数量，再次验证唯一性。</p>
     * <p>7. 遍历List，断言后一个ID大于前一个ID，验证递增性。</p>
     */
    // 标记此方法为测试方法
    @Test
    // 定义测试单线程ID唯一性的方法
    public void testIdUniqueness() {
        // 定义需要生成的ID数量
        final int COUNT = 10000;
        // 创建一个HashSet用于存储生成的ID，用于验证唯一性
        Set<Long> idSet = new HashSet<>();
        // 创建一个ArrayList用于存储生成的ID，用于验证递增性
        List<Long> idList = new ArrayList<>();
        // 循环COUNT次生成ID
        for (int i = 0; i < COUNT; i++) {
            // 调用IdGeneratorUtil.nextId()方法生成下一个ID
            long id = IdGeneratorUtil.nextId();
            // 断言idSet中不包含当前生成的ID，如果包含则表示ID重复，测试失败
            assertFalse(idSet.contains(id), "ID重复: " + id);
            // 将生成的ID添加到idSet中
            idSet.add(id);
            // 将生成的ID添加到idList中
            idList.add(id);
        }

        // 断言idSet的大小等于预期的COUNT，验证生成的ID数量是否正确，也间接验证了唯一性
        assertEquals(COUNT, idSet.size(), "生成的ID数量不正确");

        // 循环遍历idList，从第二个元素开始
        for (int i = 1; i < idList.size(); i++) {
            // 断言当前ID大于前一个ID，验证ID的递增性
            assertTrue(idList.get(i) > idList.get(i - 1), "ID不是递增的");
        }
    }

    /**
     * 测试多线程并发情况下生成的ID是否唯一
     *
     * @throws InterruptedException 如果线程等待被中断
     *
     *                              <p>实现思路:</p>
     *                              <p>创建多个线程，每个线程并发地生成一定数量的ID。</p>
     *                              <p>使用线程安全的Set来存储所有线程生成的ID。</p>
     *                              <p>使用CountDownLatch等待所有线程完成。</p>
     *                              <p>最后，验证Set的大小是否等于所有线程生成的ID总数，以此验证并发环境下的唯一性。</p>
     *
     *                              <p>实现步骤:</p>
     *                              <p>1. 定义线程数量和每个线程生成的ID数量。</p>
     *                              <p>2. 计算预期的总ID数量。</p>
     *                              <p>3. 创建一个线程安全的Set (使用Collections.synchronizedSet包装HashSet)。</p>
     *                              <p>4. 创建一个CountDownLatch，计数器初始化为线程数量。</p>
     *                              <p>5. 创建一个固定大小的线程池。</p>
     *                              <p>6. 循环启动指定数量的线程，每个线程执行生成ID的任务。</p>
     *                              <p>7. 在每个线程的任务中，循环生成指定数量的ID，并添加到线程安全的Set中。</p>
     *                              <p>8. 每个线程任务完成后，调用latch.countDown()。</p>
     *                              <p>9. 主线程调用latch.await()，等待所有线程完成。</p>
     *                              <p>10. 关闭线程池。</p>
     *                              <p>11. 断言线程安全的Set的大小等于预期的TOTAL_IDS，验证并发生成的ID数量是否正确（即没有重复）</p>
     */
    // 标记此方法为测试方法
    @Test
    // 定义测试并发ID唯一性的方法，声明可能抛出InterruptedException异常
    public void testConcurrentIdUniqueness() throws InterruptedException {
        // 定义并发测试的线程数量
        final int THREAD_COUNT = 50;
        // 定义每个线程生成的ID数量
        final int IDS_PER_THREAD = 1000;
        // 计算总共需要生成的ID数量
        final int TOTAL_IDS = THREAD_COUNT * IDS_PER_THREAD;
        // 创建一个线程安全的HashSet，用于存储并发生成的ID，验证唯一性
        Set<Long> idSet = Collections.synchronizedSet(new HashSet<>());
        // 创建一个CountDownLatch，计数器初始化为线程数量，用于等待所有线程完成
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        // 创建一个固定大小的线程池，线程数量为THREAD_COUNT
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        // 循环启动THREAD_COUNT个线程
        for (int i = 0; i < THREAD_COUNT; i++) {
            // 提交一个Runnable任务到线程池
            executorService.execute(() -> {
                // 任务的try块，用于生成ID
                try {
                    // 每个线程循环IDS_PER_THREAD次生成ID
                    for (int j = 0; j < IDS_PER_THREAD; j++) {
                        // 调用IdGeneratorUtil.nextId()方法生成ID
                        long id = IdGeneratorUtil.nextId();
                        // 将生成的ID添加到线程安全的idSet中
                        idSet.add(id);
                    }
                } finally {
                    // 任务的finally块，确保latch.countDown()被调用
                    latch.countDown();
                    // 递减CountDownLatch的计数器
                }
            });
        }
        // 主线程调用await()方法，阻塞直到CountDownLatch计数器归零
        latch.await();
        // 关闭线程池，不再接受新任务，但会执行完已提交的任务
        executorService.shutdown();
        // 断言idSet的大小等于预期的TOTAL_IDS，验证并发生成的ID数量是否正确（即没有重复）
        assertEquals(TOTAL_IDS, idSet.size(),
                // 断言失败时输出的错误信息
                "并发生成的ID有重复，预期生成 " + TOTAL_IDS + " 个唯一ID，实际生成 " + idSet.size() + " 个");
    }

    /**
     * 测试生成的ID是否符合雪花算法的结构
     *
     * <p>实现思路:</p>
     * <p>生成一个ID，验证其基本特性，如是否为正数。</p>
     * <p>打印ID的十进制和二进制形式，方便手动检查其结构是否符合雪花算法的定义（时间戳、数据中心ID、工作机器ID、序列号的位分布）。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 生成一个ID。</p>
     * <p>2. 断言ID大于0。</p>
     * <p>3. 打印ID的十进制形式。</p>
     * <p>4. 打印ID的二进制形式。</p>
     */
    // 标记此方法为测试方法
    @Test
    // 定义测试ID结构的方法
    public void testIdStructure() {
        // 调用IdGeneratorUtil.nextId()方法生成一个ID
        long id = IdGeneratorUtil.nextId();
        // 断言生成的ID大于0
        assertTrue(id > 0, "生成的ID不是正数");
        // 检查ID的位数不超过63位(不包括符号位)
        // assertTrue(id < (1L << 63), "ID超出了63位的范围"); // Incorrect assertion
        // 注释掉的错误断言，雪花算法ID是64位，最高位是符号位，通常为0表示正数，所以ID会小于2^63
        // 打印生成的ID的十进制形式
        System.out.println("ID: " + id);
        // 打印生成的ID的二进制形式，用于人工检查其结构是否符合雪花算法定义
        System.out.println("Binary: " + Long.toBinaryString(id));
    }

    /**
     * 测试字符串形式的ID生成
     *
     * <p>实现思路:</p>
     * <p>测试IdGeneratorUtil.nextIdStr()方法是否能正确生成字符串形式的ID。</p>
     * <p>验证生成的字符串非空，并且可以成功解析为Long类型，且解析后的Long值大于0。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 调用IdGeneratorUtil.nextIdStr()方法生成字符串形式的ID。</p>
     * <p>2. 断言生成的字符串非空。</p>
     * <p>3. 断言生成的字符串不为空串。</p>
     * <p>4. 尝试将字符串解析为Long类型。</p>
     * <p>5. 断言解析后的Long值大于0。</p>
     */
    // 标记此方法为测试方法
    @Test
    // 定义测试字符串形式ID生成的方法
    public void testStringIdGeneration() {
        // 调用IdGeneratorUtil.nextIdStr()方法生成字符串形式的ID
        String idStr = IdGeneratorUtil.nextIdStr();
        // 断言生成的字符串对象不为null
        assertNotNull(idStr);
        // 断言生成的字符串不为空串
        assertFalse(idStr.isEmpty());
        // 尝试将生成的字符串解析为Long类型
        long id = Long.parseLong(idStr);
        // 断言解析后的Long值大于0
        assertTrue(id > 0);
    }

    /**
     * 测试生成ID的性能
     *
     * <p>实现思路:</p>
     * <p>测试在短时间内生成大量ID的效率。</p>
     * <p>记录生成指定数量ID的开始和结束时间，计算耗时。</p>
     * <p>计算平均每秒生成的ID数量。</p>
     * <p>断言性能是否达到预期标准。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 定义需要生成的ID数量。</p>
     * <p>2. 记录开始时间戳。</p>
     * <p>3. 循环生成指定数量的ID。</p>
     * <p>4. 记录结束时间戳。</p>
     * <p>5. 计算总耗时。</p>
     * <p>6. 打印总耗时和平均每秒生成ID数。</p>
     * <p>7. 断言平均每秒生成ID数大于设定的阈值。</p>
     */
    // 标记此方法为测试方法
    @Test
    // 定义测试ID生成性能的方法
    public void testPerformance() {
        // 定义性能测试中需要生成的ID数量
        final int COUNT = 1000000;

        // 获取当前时间戳作为开始时间
        long startTime = System.currentTimeMillis();
        // 循环COUNT次生成ID
        for (int i = 0; i < COUNT; i++) {
            // 调用IdGeneratorUtil.nextId()方法生成ID
            IdGeneratorUtil.nextId();
        }
        // 获取当前时间戳作为结束时间
        long endTime = System.currentTimeMillis();
        // 计算总耗时（毫秒）
        long duration = endTime - startTime;
        // 打印总耗时
        System.out.println("生成 " + COUNT + " 个ID耗时: " + duration + " 毫秒");
        // 计算并打印平均每秒生成的ID数量
        System.out.println("平均每秒生成ID数: " + (COUNT * 1000L / duration));
        // 断言平均每秒生成ID数大于100000，验证性能是否达标
        assertTrue((COUNT * 1000L / duration) > 100000, "ID生成性能不达标");
    }

    /**
     * 测试生成一百万个分布式ID，验证没有重复。
     *
     * <p>实现思路:</p>
     * <p>生成一百万个ID，使用Set集合来验证这些ID是否全部唯一。</p>
     *
     * <p>实现步骤:</p>
     * <p>1. 定义生成ID的数量为一百万。</p>
     * <p>2. 创建一个HashSet用于存储ID。</p>
     * <p>3. 循环生成一百万个ID，并添加到Set中。</p>
     * <p>4. 断言Set的大小等于一百万，验证唯一性。</p>
     */
    @Test
    public void testMillionIdUniqueness() {
        // 定义需要生成的ID数量：一百万
        final int COUNT = 1000000;
        // 创建一个HashSet用于存储生成的ID，用于验证唯一性
        Set<Long> idSet = new HashSet<>();

        // 循环COUNT次生成ID并添加到Set中
        for (int i = 0; i < COUNT; i++) {
            long id = IdGeneratorUtil.nextId();
            idSet.add(id);
        }
        // 断言idSet的大小等于预期的COUNT，验证生成的ID数量是否正确，即没有重复
        assertEquals(COUNT, idSet.size(), "生成的百万个ID有重复");
    }
}