package com.hao.strategyengine.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ==========================================================
 * 🧩 MySQL负载监控器 (MySQL Load Monitor)
 * ==========================================================
 * 【设计目的 / Purpose】
 * 该组件用于周期性监控 MySQL 数据库的连接与线程运行状态，
 * 以便及时发现连接耗尽、线程阻塞、过载等问题。
 * 输出日志采用中英双语，便于团队阅读与跨语言协作。
 * <p>
 * 【主要指标 / Key Metrics】
 * - Threads_connected : 当前数据库活跃连接数 (Active connections)
 * - Threads_running   : 当前正在执行SQL的线程数 (Running SQL threads)
 * - max_connections   : 数据库允许的最大连接数 (Configured connection limit)
 * <p>
 * 【判定规则 / Health Rules】
 * ✅ Threads_connected / max_connections < 0.7       → 稳定 / Stable
 * ⚠️ Threads_running / CPU核心数 在 [2, 3] 之间      → 高负载 / High Load
 * ❌ Threads_running > CPU核心数 × 3                → 过载 / Overloaded
 * <p>
 * 【执行频率 / Frequency】
 * 默认每 30 秒执行一次，可根据需求调整。
 */

/**
 * ==========================================================
 * ⚙️ MySQL连接数设置原则 (How to Determine max_connections)
 * ==========================================================
 *
 * 【1️⃣ 基本概念】
 *   MySQL 的每个连接对应一个独立线程（Thread-per-Connection 模型）。
 *   因此连接数并不是越多越好，过多会导致：
 *     - CPU 上下文切换频繁 (Context Switch)
 *     - 内存消耗激增 (Memory Overhead)
 *     - InnoDB 全局锁竞争 (Global Lock Contention)
 *
 * 【2️⃣ 设置方法】
 *   参数名：max_connections
 *   示例配置：my.cnf
 *   --------------------------------------------------------
 *   [mysqld]
 *   max_connections = 600
 *   --------------------------------------------------------
 *
 * 【3️⃣ 理论估算公式】
 *   推荐范围：100 ～ 1000（视业务规模而定）
 *
 *   max_connections ≈ 可分配内存(MB) / 单连接平均占用(MB)
 *
 *   单连接平均占用 = 线程栈 + 会话缓存 + 排序/Join缓冲 ≈ 1~2 MB
 *
 *   举例：
 *     - 服务器总内存：32 GB
 *     - 预留给InnoDB缓冲池：20 GB
 *     - 剩余可分配内存：12 GB
 *     - 单连接约占 1.5 MB
 *     → 12GB / 1.5MB ≈ 800  → 建议设置 max_connections = 600~800
 *
 * 【4️⃣ CPU 并发上限考虑】
 *   MySQL 属于线程驱动型系统：
 *     - 并发执行线程数建议 ≤ CPU核心数 × 2
 *     - 其余线程会被挂起，频繁调度会拖慢整体性能。
 *   示例：
 *     CPU 16 核 → 推荐活跃线程 32 以内，连接总数 ≤ 600。
 *
 * 【5️⃣ 与连接池协同设置】
 *   应用层连接池 (如 HikariCP) 推荐：
 *     - 每个微服务连接池大小：50~100
 *     - 多实例部署时：max_connections ≥ 连接池总和 × 1.2
 *     例：6 个服务 × 50 = 300 → max_connections = 360~400
 *
 * 【6️⃣ 运行期动态观测】
 *   使用以下SQL观测当前连接负载：
 *     SHOW GLOBAL STATUS LIKE 'Threads_connected';
 *     SHOW GLOBAL STATUS LIKE 'Threads_running';
 *     SHOW VARIABLES LIKE 'max_connections';
 *
 *   判定标准：
 *     - Threads_connected / max_connections < 0.7 → 稳定
 *     - Threads_running / CPU核数 < 2            → 健康
 *     - Threads_running > CPU核数 × 3             → 过载
 *
 * 【7️⃣ 最佳实践】
 *   ✅ 保持连接池重用（不要频繁创建/销毁连接）
 *   ✅ 定期监控连接增长趋势（通过本监控类）
 *   ✅ 避免在同一MySQL上堆多个高QPS微服务
 *
 * ==========================================================
 */
@Slf4j
@Component
public class MysqlLoadMonitor {

    @Autowired
    private SqlSessionFactory sqlSessionFactory; // MyBatis 提供的 SqlSessionFactory，可获取数据库连接

    /**
     * 定时任务入口方法
     * ----------------------------------------------------------
     * 每隔30秒执行一次，监控当前MySQL连接与线程负载情况。
     * 使用MyBatis的数据源连接直接执行MySQL内部状态查询SQL。
     */
    @Scheduled(fixedRate = 30000)
    public void monitor() {
        // try-with-resources 自动关闭连接，防止资源泄漏
        try (Connection conn = sqlSessionFactory.openSession().getConnection()) {

            // 1️⃣ 获取核心指标
            long threadsConnected = queryMetricValue(conn, "Threads_connected"); // 当前活跃连接数
            long threadsRunning = queryMetricValue(conn, "Threads_running");   // 正在执行SQL的线程数
            long maxConnections = queryMetricValue(conn, "max_connections");   // 数据库最大连接数
            int cpuCores = Runtime.getRuntime().availableProcessors();            // 当前服务所在机器的CPU核心数

            // 2️⃣ 计算连接使用率与线程压力比
            double connectionUsage = (double) threadsConnected / maxConnections;
            double threadPressure = (double) threadsRunning / cpuCores;
            // 计算后格式化为两位小数
            String connectionUsageStr = String.format("%.2f", connectionUsage * 100);
            String threadPressureStr = String.format("%.2f", threadPressure);

            // 打印监控日志（中英文）
            log.info("【MySQL实时监控 | Real-Time MySQL Monitor】");
            log.info("当前连接数 (Threads_connected): {}", threadsConnected);
            log.info("当前运行线程数 (Threads_running): {}", threadsRunning);
            log.info("最大连接数 (max_connections): {}", maxConnections);
            log.info("CPU核心数 (CPU Cores): {}", cpuCores);
            log.info("连接使用率 (Connection Usage): {}%", connectionUsageStr);
            log.info("线程压力比 (Thread Pressure): {}", threadPressureStr);


            // 4️⃣ 健康度判定逻辑（Health Status Evaluation）
            if (connectionUsage < 0.7 && threadPressure < 2) {
                log.info("✅ 数据库状态稳定 / Database Status: STABLE");
            } else if (threadPressure >= 2 && threadPressure <= 3) {
                log.warn("⚠️ 数据库处于高负载 / Database under HIGH LOAD");
            } else if (threadPressure > 3) {
                log.error("❌ 数据库过载，请检查慢查询或连接池配置 / Database OVERLOADED, please inspect slow queries or pool sizing.");
            }

        } catch (Exception e) {
            log.error("❌ 监控任务执行失败 / Monitor task failed", e);
        }
    }

    /**
     * 对外暴露的指标查询接口
     * ----------------------------------------------------------
     * 可用于单元测试、监控接口或自定义告警模块调用。
     *
     * @param metricName 指标名称，可选值：
     *                   Threads_connected / Threads_running / max_connections
     * @return 对应指标的数值
     * @throws Exception 当数据库连接或SQL执行失败时抛出
     */
    public long queryMetricValue(String metricName) throws Exception {
        try (Connection conn = sqlSessionFactory.openSession().getConnection()) {
            return queryMetricValue(conn, metricName);
        }
    }

    /**
     * 内部通用查询方法 (Internal Metric Query)
     * ----------------------------------------------------------
     * 通过执行 MySQL 系统命令（SHOW STATUS / SHOW VARIABLES）获取运行时指标。
     *
     * @param conn        当前数据库连接
     * @param metricName  指标名称
     * @return 指标值 (long)
     * @throws Exception  执行SQL或解析结果失败时抛出异常
     */
    private long queryMetricValue(Connection conn, String metricName) throws Exception {
        // 根据指标名称动态选择SQL语句
        String sql;
        if ("max_connections".equalsIgnoreCase(metricName)) {
            sql = "SHOW VARIABLES LIKE 'max_connections'";
        } else if ("Threads_connected".equalsIgnoreCase(metricName)) {
            sql = "SHOW GLOBAL STATUS LIKE 'Threads_connected'";
        } else if ("Threads_running".equalsIgnoreCase(metricName)) {
            sql = "SHOW GLOBAL STATUS LIKE 'Threads_running'";
        } else {
            // 未知指标直接抛出异常（明确告知调用方）
            throw new IllegalArgumentException("未知指标 / Unknown metric: " + metricName);
        }

        // 执行SQL语句
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // SHOW命令的结果集一般为两列：Variable_name / Value
            if (rs.next()) {
                // 解析第二列的Value字段为数值型
                return Long.parseLong(rs.getString("Value"));
            }
        }

        // 未查询到结果返回0，保证方法健壮性
        return 0L;
    }
}
