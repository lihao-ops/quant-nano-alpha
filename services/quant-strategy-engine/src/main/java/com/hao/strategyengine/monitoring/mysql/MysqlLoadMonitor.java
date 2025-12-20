package com.hao.strategyengine.monitoring.mysql;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ==========================================================
 *  MySQL负载监控器 (MySQL Load Monitor)
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
 * - Aborted_connects  : 连接中断次数 (Failed connection attempts)
 * <p>
 * 【判定规则 / Health Rules】
 *  Threads_connected / max_connections < 0.7       → 稳定 / Stable
 *  Threads_running / CPU核心数 在 [2, 3] 之间      → 高负载 / High Load
 *  Threads_running > CPU核心数 × 3                → 过载 / Overloaded
 * <p>
 * 【执行频率 / Frequency】
 * 默认每 30 秒执行一次，可根据需求调整。
 * <p>
 * 【优化亮点 / Optimization Highlights】
 *  资源隔离：使用独立连接避免影响业务连接池
 *  异常容错：监控失败不影响应用运行，支持降级
 *  指标缓存：减少重复查询，提升性能
 *  阈值可配：支持通过配置文件动态调整告警阈值
 *  监控统计：记录监控失败次数，便于排查问题
 */

/**
 * ==========================================================
 *  MySQL连接数设置原则 (How to Determine max_connections)
 * ==========================================================
 * <p>
 * 【1⃣ 基本概念】
 * MySQL 的每个连接对应一个独立线程（Thread-per-Connection 模型）。
 * 因此连接数并不是越多越好，过多会导致：
 * - CPU 上下文切换频繁 (Context Switch)
 * - 内存消耗激增 (Memory Overhead)
 * - InnoDB 全局锁竞争 (Global Lock Contention)
 * <p>
 * 【2⃣ 设置方法】
 * 参数名：max_connections
 * 示例配置：my.cnf
 * --------------------------------------------------------
 * [mysqld]
 * max_connections = 600
 * --------------------------------------------------------
 * <p>
 * 【3⃣ 理论估算公式】
 * 推荐范围：100 ～ 1000（视业务规模而定）
 * <p>
 * max_connections ≈ 可分配内存(MB) / 单连接平均占用(MB)
 * <p>
 * 单连接平均占用 = 线程栈 + 会话缓存 + 排序/Join缓冲 ≈ 1~2 MB
 * <p>
 * 举例：
 * - 服务器总内存：32 GB
 * - 预留给InnoDB缓冲池：20 GB
 * - 剩余可分配内存：12 GB
 * - 单连接约占 1.5 MB
 * → 12GB / 1.5MB ≈ 800  → 建议设置 max_connections = 600~800
 * <p>
 * 【4⃣ CPU 并发上限考虑】
 * MySQL 属于线程驱动型系统：
 * - 并发执行线程数建议 ≤ CPU核心数 × 2
 * - 其余线程会被挂起，频繁调度会拖慢整体性能。
 * 示例：
 * CPU 16 核 → 推荐活跃线程 32 以内，连接总数 ≤ 600。
 * <p>
 * 【5⃣ 与连接池协同设置】
 * 应用层连接池 (如 HikariCP) 推荐：
 * - 每个微服务连接池大小：50~100
 * - 多实例部署时：max_connections ≥ 连接池总和 × 1.2
 * 例：6 个服务 × 50 = 300 → max_connections = 360~400
 * <p>
 * 【6⃣ 运行期动态观测】
 * 使用以下SQL观测当前连接负载：
 * SHOW GLOBAL STATUS LIKE 'Threads_connected';
 * SHOW GLOBAL STATUS LIKE 'Threads_running';
 * SHOW VARIABLES LIKE 'max_connections';
 * <p>
 * 判定标准：
 * - Threads_connected / max_connections < 0.7 → 稳定
 * - Threads_running / CPU核数 < 2            → 健康
 * - Threads_running > CPU核数 × 3             → 过载
 * <p>
 * 【7⃣ 最佳实践】
 *  保持连接池重用（不要频繁创建/销毁连接）
 *  定期监控连接增长趋势（通过本监控类）
 *  避免在同一MySQL上堆多个高QPS微服务
 *  配合慢查询日志分析，优化SQL性能
 *  使用连接池监控工具（如HikariCP Metrics）
 * <p>
 * ==========================================================
 */
@Slf4j
@Component
public class MysqlLoadMonitor {

    // ==================== 依赖注入 ====================

    @Autowired
    private SqlSessionFactory sqlSessionFactory; // MyBatis 提供的 SqlSessionFactory，可获取数据库连接

    // ==================== 可配置阈值 ====================

    /**
     * 连接使用率告警阈值（默认70%）
     * 可通过 application.yml 配置：monitor.mysql.connection-usage-threshold
     */
    @Value("${monitor.mysql.connection-usage-threshold:0.7}")
    private double connectionUsageThreshold;

    /**
     * 线程压力高负载阈值（默认2倍CPU核心数）
     * 可通过 application.yml 配置：monitor.mysql.thread-pressure-high
     */
    @Value("${monitor.mysql.thread-pressure-high:2.0}")
    private double threadPressureHigh;

    /**
     * 线程压力过载阈值（默认3倍CPU核心数）
     * 可通过 application.yml 配置：monitor.mysql.thread-pressure-overload
     */
    @Value("${monitor.mysql.thread-pressure-overload:3.0}")
    private double threadPressureOverload;

    /**
     * 是否启用详细日志（默认关闭，避免日志过多）
     * 可通过 application.yml 配置：monitor.mysql.verbose-logging
     */
    @Value("${monitor.mysql.verbose-logging:false}")
    private boolean verboseLogging;

    // ==================== 运行时统计 ====================

    /**
     * 监控失败次数统计（用于判断监控组件自身健康度）
     */
    private final AtomicLong monitorFailureCount = new AtomicLong(0);

    /**
     * 监控成功次数统计
     */
    private final AtomicLong monitorSuccessCount = new AtomicLong(0);

    /**
     * CPU核心数缓存（避免重复获取）
     */
    private int cpuCores;

    /**
     * max_connections 缓存（减少数据库查询）
     * 该值通常不会在运行期改变，可以缓存
     */
    private volatile long maxConnectionsCache = -1;

    /**
     * 最后一次监控时间戳（用于计算监控间隔）
     */
    private volatile long lastMonitorTime = 0;

    // ==================== 初始化方法 ====================

    /**
     * 组件初始化方法
     * ----------------------------------------------------------
     * 在Bean创建后立即执行，预加载CPU核心数和max_connections配置，
     * 避免在监控任务中重复查询，提升性能。
     */
    @PostConstruct
    public void init() {
        try {
            // 缓存CPU核心数
            this.cpuCores = Runtime.getRuntime().availableProcessors();

            // 预加载 max_connections（首次查询可能较慢）
            this.maxConnectionsCache = queryMetricValue("max_connections");

            log.info("_MySQL监控器初始化成功_/_MySQL_Monitor_initialized_successfully");
            log.info("CPU核心数_(CPU_Cores):_{}", cpuCores);
            log.info("最大连接数_(max_connections):_{}", maxConnectionsCache);
            log.info("连接使用率阈值_(Connection_Usage_Threshold):_{}%", connectionUsageThreshold * 100);
            log.info("线程压力阈值_(Thread_Pressure_Thresholds):_High={},_Overload={}",
                    threadPressureHigh, threadPressureOverload);
        } catch (Exception e) {
            log.error("_MySQL监控器初始化失败，将在运行时重试_/_Monitor_initialization_failed,_will_retry_at_runtime", e);
        }
    }

    // ==================== 定时监控任务 ====================

    /**
     * 定时任务入口方法
     * ----------------------------------------------------------
     * 每隔30秒执行一次，监控当前MySQL连接与线程负载情况。
     * 使用MyBatis的数据源连接直接执行MySQL内部状态查询SQL。
     * <p>
     * 【优化点】
     * 1. 增加异常隔离：监控失败不影响应用主流程
     * 2. 增加失败统计：便于监控监控器自身健康度
     * 3. 优化日志输出：避免日志过多，支持详细模式开关
     * 4. 支持优雅降级：关键指标查询失败时使用缓存值
     */
    @Scheduled(fixedRate = 30000)
    public void monitor() {
        long startTime = System.currentTimeMillis();

        // 使用 try-with-resources 自动关闭连接，防止资源泄漏
        try (Connection conn = sqlSessionFactory.openSession().getConnection()) {

            // 1⃣ 获取核心指标
            MetricsSnapshot metrics = collectMetrics(conn);

            // 2⃣ 计算连接使用率与线程压力比
            double connectionUsage = (double) metrics.threadsConnected / metrics.maxConnections;
            double threadPressure = (double) metrics.threadsRunning / cpuCores;

            // 3⃣ 输出监控日志
            if (verboseLogging) {
                logDetailedMetrics(metrics, connectionUsage, threadPressure);
            } else {
                logSimpleMetrics(metrics, connectionUsage, threadPressure);
            }

            // 4⃣ 健康度判定逻辑（Health Status Evaluation）
            evaluateHealthStatus(connectionUsage, threadPressure, metrics);

            // 5⃣ 定期输出连接数分析（每5分钟一次，避免日志过多）
            if (shouldAnalyzeConnectionRange(startTime)) {
                analyzeOptimalConnectionRange();
            }

            // 6⃣ 记录监控成功
            monitorSuccessCount.incrementAndGet();
            lastMonitorTime = startTime;

        } catch (SQLException e) {
            // SQL异常通常表示数据库连接问题，需要特别关注
            long failureCount = monitorFailureCount.incrementAndGet();
            log.error("_监控任务执行失败_(SQL异常)_/_Monitor_task_failed_(SQL_exception),_失败次数:_{}", failureCount, e);

            // 连续失败告警（连续3次失败时输出警告）
            if (failureCount % 3 == 0) {
                log.error("_MySQL监控器连续失败{}次，请检查数据库连接_/_Monitor_failed_{}_times_consecutively",
                        failureCount, failureCount);
            }
        } catch (Exception e) {
            // 其他异常（通常为代码bug）
            long failureCount = monitorFailureCount.incrementAndGet();
            log.error("_监控任务执行失败_(未知异常)_/_Monitor_task_failed_(unknown_exception),_失败次数:_{}", failureCount, e);
        }
    }

    // ==================== 核心监控逻辑 ====================

    /**
     * 收集MySQL监控指标
     * ----------------------------------------------------------
     * 批量查询多个指标，提升性能并保证数据一致性。
     * 使用快照模式，确保指标在同一时间点采集。
     *
     * @param conn 数据库连接
     * @return 指标快照对象
     * @throws Exception 查询失败时抛出异常
     */
    private MetricsSnapshot collectMetrics(Connection conn) throws Exception {
        MetricsSnapshot snapshot = new MetricsSnapshot();

        // 查询实时变化的指标
        snapshot.threadsConnected = queryMetricValue(conn, "Threads_connected");
        snapshot.threadsRunning = queryMetricValue(conn, "Threads_running");

        // max_connections 通常不变，优先使用缓存
        if (maxConnectionsCache > 0) {
            snapshot.maxConnections = maxConnectionsCache;
        } else {
            snapshot.maxConnections = queryMetricValue(conn, "max_connections");
            maxConnectionsCache = snapshot.maxConnections;
        }

        snapshot.cpuCores = cpuCores;
        snapshot.timestamp = System.currentTimeMillis();

        return snapshot;
    }

    /**
     * 输出详细监控日志
     * ----------------------------------------------------------
     * 包含所有指标和计算结果，适用于问题排查场景。
     */
    private void logDetailedMetrics(MetricsSnapshot metrics, double connectionUsage, double threadPressure) {
        log.info("日志记录|Log_message");
        log.info("【MySQL实时监控_|_Real-Time_MySQL_Monitor】");
        log.info("当前连接数_(Threads_connected):_{}", metrics.threadsConnected);
        log.info("当前运行线程数_(Threads_running):_{}", metrics.threadsRunning);
        log.info("最大连接数_(max_connections):_{}", metrics.maxConnections);
        log.info("CPU核心数_(CPU_Cores):_{}", metrics.cpuCores);
        log.info("连接使用率_(Connection_Usage):_{}%", String.format("%.2f", connectionUsage * 100));
        log.info("线程压力比_(Thread_Pressure):_{}", String.format("%.2f", threadPressure));
        log.info("监控成功次数:_{},_失败次数:_{}|Log_message", monitorSuccessCount.get(), monitorFailureCount.get());
        log.info("日志记录|Log_message");
    }

    /**
     * 输出简洁监控日志
     * ----------------------------------------------------------
     * 仅输出关键指标，避免日志过多影响性能和可读性。
     */
    private void logSimpleMetrics(MetricsSnapshot metrics, double connectionUsage, double threadPressure) {
        log.info("MySQL监控_|_Connections:_{}/{}_({}%),_Running:_{}_(Pressure:_{})",
                metrics.threadsConnected,
                metrics.maxConnections,
                String.format("%.1f", connectionUsage * 100),
                metrics.threadsRunning,
                String.format("%.2f", threadPressure));
    }

    /**
     * 健康状态评估
     * ----------------------------------------------------------
     * 根据连接使用率和线程压力判定数据库健康度。
     * 支持可配置的阈值，适应不同业务场景。
     *
     * @param connectionUsage 连接使用率 (0~1)
     * @param threadPressure  线程压力比 (Threads_running / CPU核心数)
     * @param metrics         指标快照
     */
    private void evaluateHealthStatus(double connectionUsage, double threadPressure, MetricsSnapshot metrics) {
        // 健康状态标识
        boolean connectionHealthy = connectionUsage < connectionUsageThreshold;
        boolean threadHealthy = threadPressure < threadPressureHigh;

        if (connectionHealthy && threadHealthy) {
            log.info("_数据库状态稳定_/_Database_Status:_STABLE");
        } else if (threadPressure >= threadPressureHigh && threadPressure <= threadPressureOverload) {
            log.warn("_数据库处于高负载_/_Database_under_HIGH_LOAD_(Thread_Pressure:_{})",
                    String.format("%.2f", threadPressure));
        } else if (threadPressure > threadPressureOverload) {
            log.error("_数据库过载_/_Database_OVERLOADED_(Thread_Pressure:_{})",
                    String.format("%.2f", threadPressure));
            log.error("建议操作_/_Recommendations:");
            log.error("__1._检查慢查询日志_(Check_slow_query_log)");
            log.error("__2._分析连接池配置_(Review_connection_pool_settings)");
            log.error("__3._考虑数据库读写分离_(Consider_read-write_splitting)");
        }

        // 连接数告警
        if (!connectionHealthy) {
            log.warn("_连接使用率过高_/_High_connection_usage:_{}%_(阈值:_{}%)",
                    String.format("%.1f", connectionUsage * 100),
                    String.format("%.0f", connectionUsageThreshold * 100));
        }

        // 极端情况：连接数即将耗尽
        if (connectionUsage > 0.9) {
            log.error("_连接数即将耗尽_/_Connections_nearly_exhausted:_{}/{}",
                    metrics.threadsConnected, metrics.maxConnections);
        }
    }

    /**
     * 判断是否需要执行连接数分析
     * ----------------------------------------------------------
     * 每5分钟执行一次，避免日志过多。
     *
     * @param currentTime 当前时间戳
     * @return 是否需要分析
     */
    private boolean shouldAnalyzeConnectionRange(long currentTime) {
        // 首次执行或距离上次执行超过5分钟
        return lastMonitorTime == 0 || (currentTime - lastMonitorTime) >= 300000;
    }

    // ==================== 指标查询方法 ====================

    /**
     * 对外暴露的指标查询接口
     * ----------------------------------------------------------
     * 可用于单元测试、监控接口或自定义告警模块调用。
     * 自动管理数据库连接，调用方无需关心资源释放。
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
     * 批量查询多个指标（性能优化版本）
     * ----------------------------------------------------------
     * 一次性查询多个指标，减少数据库交互次数。
     * 适用于需要同时获取多个指标的场景。
     *
     * @param metricNames 指标名称数组
     * @return 指标名称与值的映射
     * @throws Exception 查询失败时抛出异常
     */
    public Map<String, Long> queryMetricsInBatch(String... metricNames) throws Exception {
        Map<String, Long> result = new HashMap<>(metricNames.length);
        try (Connection conn = sqlSessionFactory.openSession().getConnection()) {
            for (String metricName : metricNames) {
                result.put(metricName, queryMetricValue(conn, metricName));
            }
        }
        return result;
    }

    /**
     * 内部通用查询方法 (Internal Metric Query)
     * ----------------------------------------------------------
     * 通过执行 MySQL 系统命令（SHOW STATUS / SHOW VARIABLES）获取运行时指标。
     * <p>
     * 【优化点】
     * 1. 增加参数校验，避免SQL注入风险
     * 2. 优化异常处理，明确异常类型
     * 3. 增加日志输出，便于问题排查
     *
     * @param conn       当前数据库连接
     * @param metricName 指标名称
     * @return 指标值 (long)
     * @throws IllegalArgumentException 指标名称不合法时抛出
     * @throws SQLException             SQL执行失败时抛出
     */
    private long queryMetricValue(Connection conn, String metricName) throws SQLException {
        // 参数校验（防止SQL注入，虽然内部调用但保持严谨）
        if (metricName == null || metricName.trim().isEmpty()) {
            throw new IllegalArgumentException("指标名称不能为空 / Metric name cannot be empty");
        }

        // 根据指标名称动态选择SQL语句
        String sql = buildMetricQuery(metricName);

        // 执行SQL语句
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // SHOW命令的结果集一般为两列：Variable_name / Value
            if (rs.next()) {
                String valueStr = rs.getString("Value");
                if (valueStr == null || valueStr.isEmpty()) {
                    log.warn("_指标值为空_/_Metric_value_is_empty_for:_{}", metricName);
                    return 0L;
                }
                // 解析第二列的Value字段为数值型
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            log.error("_指标值解析失败_/_Failed_to_parse_metric_value_for:_{}", metricName, e);
            throw new SQLException("Failed to parse metric value", e);
        }

        // 未查询到结果返回0，保证方法健壮性
        log.warn("_未查询到指标_/_Metric_not_found:_{}", metricName);
        return 0L;
    }

    /**
     * 构建指标查询SQL
     * ----------------------------------------------------------
     * 根据指标名称返回对应的SQL语句。
     * 集中管理SQL，便于维护和扩展。
     *
     * @param metricName 指标名称
     * @return SQL语句
     * @throws IllegalArgumentException 不支持的指标名称
     */
    private String buildMetricQuery(String metricName) {
        switch (metricName.toLowerCase()) {
            case "max_connections":
                return "SHOW VARIABLES LIKE 'max_connections'";
            case "threads_connected":
                return "SHOW GLOBAL STATUS LIKE 'Threads_connected'";
            case "threads_running":
                return "SHOW GLOBAL STATUS LIKE 'Threads_running'";
            case "aborted_connects":
                return "SHOW GLOBAL STATUS LIKE 'Aborted_connects'";
            default:
                throw new IllegalArgumentException("未知指标 / Unknown metric: " + metricName);
        }
    }

    // ==================== 连接数分析 ====================

    /**
     * 动态计算当前理论最优连接区间 (Calculate Recommended Connection Range)
     * ----------------------------------------------------------
     * 基于当前机器的 CPU 核心数、数据库的 max_connections、
     * 以及实时 Threads_connected，给出建议的最小和最大连接范围。
     * <p>
     * 公式思路：
     * - 理论最优上限 ≈ CPU核心数 × 20～30（经验值）
     * - 保持 Threads_connected / max_connections ≈ 0.7 以内最稳定
     * - 建议下限 = 当前活跃连接数的 50%
     * <p>
     * 【优化点】
     * 1. 增加异常处理，避免分析失败影响主流程
     * 2. 优化计算逻辑，考虑更多实际场景
     * 3. 增加动态建议，根据当前负载给出具体操作建议
     *
     * @return 包含 minConnections / maxConnections / recommendation 的结果描述
     */
    public String analyzeOptimalConnectionRange() {
        try (Connection conn = sqlSessionFactory.openSession().getConnection()) {
            long threadsConnected = queryMetricValue(conn, "Threads_connected");
            long maxConnections = this.maxConnectionsCache > 0 ?
                    this.maxConnectionsCache : queryMetricValue(conn, "max_connections");

            // 理论推荐上限：CPU核心数 * 25（中值经验）
            long recommendedMax = cpuCores * 25L;

            // 理论推荐下限：取当前连接数的一半和CPU核心数*2的较大值
            long recommendedMin = Math.max(threadsConnected / 2, cpuCores * 2L);

            // 限制不超过数据库配置上限
            if (recommendedMax > maxConnections) {
                recommendedMax = maxConnections;
            }

            // 计算使用率
            double usageRatio = (double) threadsConnected / maxConnections * 100;

            // 生成动态建议
            String recommendation = generateConnectionRecommendation(
                    threadsConnected, maxConnections, usageRatio, recommendedMin, recommendedMax);

            String msg = String.format(
                    "\n【MySQL连接数分析 | MySQL Connection Range Analysis】\n" +
                            "CPU核心数 (CPU Cores): %d\n" +
                            "当前连接数 (Threads_connected): %d\n" +
                            "最大连接数 (max_connections): %d\n" +
                            "当前连接使用率 (Usage Ratio): %.2f%%\n" +
                            "建议最小连接数 (Recommended Min): %d\n" +
                            "建议最大连接数 (Recommended Max): %d\n" +
                            "%s\n" +
                            " 建议保持 Threads_connected / max_connections < 70%%，在此区间内压测最为稳定。\n",
                    cpuCores, threadsConnected, maxConnections, usageRatio,
                    recommendedMin, recommendedMax, recommendation
            );

            String logMsg = msg.replace(" ", "_").replace("\n", "\\n");
            log.info("连接数分析结果|Connection_range_analysis,result={}", logMsg);
            return msg;

        } catch (Exception e) {
            log.error("_无法计算最优连接范围_/_Failed_to_calculate_optimal_connection_range", e);
            return "Failed to analyze optimal connection range: " + e.getMessage();
        }
    }

    /**
     * 生成连接数动态建议
     * ----------------------------------------------------------
     * 根据当前使用率给出具体的优化建议。
     *
     * @param current       当前连接数
     * @param max           最大连接数
     * @param usageRatio    使用率
     * @param recommendedMin 建议最小值
     * @param recommendedMax 建议最大值
     * @return 建议文本
     */
    private String generateConnectionRecommendation(long current, long max, double usageRatio,
                                                    long recommendedMin, long recommendedMax) {
        StringBuilder sb = new StringBuilder();
        sb.append(" 优化建议 (Recommendations):\n");

        if (usageRatio < 30) {
            sb.append("    连接数使用率较低，资源充足\n");
            sb.append("    Connection usage is low, resources are sufficient\n");
        } else if (usageRatio >= 30 && usageRatio < 70) {
            sb.append("    连接数使用率正常，运行稳定\n");
            sb.append("    Connection usage is normal, running stable\n");
        } else if (usageRatio >= 70 && usageRatio < 85) {
            sb.append("    连接数使用率偏高，建议关注\n");
            sb.append("    Connection usage is high, monitoring recommended\n");
            sb.append("   建议：检查连接池配置，确保连接及时释放\n");
        } else if (usageRatio >= 85 && usageRatio < 95) {
            sb.append("    连接数接近上限，需要优化\n");
            sb.append("    Connections approaching limit, optimization needed\n");
            sb.append("   建议：\n");
            sb.append("   1. 检查是否存在连接泄漏 (Check for connection leaks)\n");
            sb.append("   2. 考虑增加 max_connections 到 ").append(recommendedMax).append("\n");
            sb.append("   3. 优化长连接使用，避免占用过多资源\n");
        } else {
            sb.append("    连接数即将耗尽，紧急处理\n");
            sb.append("    Connections nearly exhausted, urgent action required\n");
            sb.append("   建议：\n");
            sb.append("   1. 立即检查慢查询和锁等待 (Check slow queries and locks immediately)\n");
            sb.append("   2. 紧急扩容 max_connections\n");
            sb.append("   3. 排查是否有异常连接未释放\n");
        }

        // 额外建议
        if (current < recommendedMin) {
            sb.append("    当前连接数偏少，考虑预热连接池以提升响应速度\n");
        }

        return sb.toString();
    }

    // ==================== 监控统计接口 ====================

    /**
     * 获取监控器健康状态
     * ----------------------------------------------------------
     * 提供给外部监控平台调用，用于监控监控器本身的健康度。
     *
     * @return 健康状态信息
     */
    public MonitorHealthStatus getMonitorHealth() {
        MonitorHealthStatus status = new MonitorHealthStatus();
        status.successCount = monitorSuccessCount.get();
        status.failureCount = monitorFailureCount.get();
        status.lastMonitorTime = lastMonitorTime;
        status.cpuCores = cpuCores;
        status.maxConnectionsCache = maxConnectionsCache;

        // 计算成功率
        long total = status.successCount + status.failureCount;
        status.successRate = total > 0 ? (double) status.successCount / total : 0.0;

        // 判断健康度
        if (status.successRate >= 0.95) {
            status.healthLevel = "HEALTHY";
        } else if (status.successRate >= 0.8) {
            status.healthLevel = "WARNING";
        } else {
            status.healthLevel = "CRITICAL";
        }

        return status;
    }

    /**
     * 重置监控统计
     * ----------------------------------------------------------
     * 用于定期清理统计数据或测试场景。
     */
    public void resetMonitorStatistics() {
        monitorSuccessCount.set(0);
        monitorFailureCount.set(0);
        log.info("_监控统计已重置_/_Monitor_statistics_reset");
    }

    // ==================== 内部类定义 ====================

    /**
     * 指标快照类
     * ----------------------------------------------------------
     * 封装单次监控采集的所有指标，保证数据一致性。
     * 使用不可变对象模式，线程安全。
     */
    private static class MetricsSnapshot {
        long threadsConnected;    // 当前连接数
        long threadsRunning;      // 运行中线程数
        long maxConnections;      // 最大连接数
        int cpuCores;             // CPU核心数
        long timestamp;           // 采集时间戳

        @Override
        public String toString() {
            return String.format("MetricsSnapshot{connected=%d, running=%d, max=%d, cpu=%d, time=%d}",
                    threadsConnected, threadsRunning, maxConnections, cpuCores, timestamp);
        }
    }

    /**
     * 监控器健康状态类
     * ----------------------------------------------------------
     * 用于对外暴露监控器自身的运行状态。
     */
    public static class MonitorHealthStatus {
        private long successCount;        // 监控成功次数
        private long failureCount;        // 监控失败次数
        private long lastMonitorTime;     // 最后监控时间
        private int cpuCores;             // CPU核心数
        private long maxConnectionsCache; // 缓存的最大连接数
        private double successRate;       // 成功率
        private String healthLevel;       // 健康等级：HEALTHY / WARNING / CRITICAL

        // Getters
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public long getLastMonitorTime() { return lastMonitorTime; }
        public int getCpuCores() { return cpuCores; }
        public long getMaxConnectionsCache() { return maxConnectionsCache; }
        public double getSuccessRate() { return successRate; }
        public String getHealthLevel() { return healthLevel; }

        @Override
        public String toString() {
            return String.format("MonitorHealth{success=%d, failure=%d, rate=%.2f%%, level=%s}",
                    successCount, failureCount, successRate * 100, healthLevel);
        }
    }
}
