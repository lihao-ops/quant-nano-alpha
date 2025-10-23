package com.hao.strategyengine.common.model.market;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketData
 *
 * <p>市场行情数据对象，用于封装单个交易标的在某一时间点的市场信息。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>symbol - 交易标的代码，例如股票 "AAPL"</li>
 *     <li>price - 当前价格</li>
 *     <li>volume - 成交量</li>
 *     <li>high - 当日最高价</li>
 *     <li>low - 当日最低价</li>
 *     <li>open - 当日开盘价</li>
 *     <li>timestamp - 数据时间戳</li>
 *     <li>indicators - 技术指标集合，可存储MACD、RSI等自定义指标，key 为指标名，value 为指标值</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * MarketData data = new MarketData();
 * data.setSymbol("AAPL");
 * data.setPrice(new BigDecimal("178.35"));
 * data.setVolume(new BigDecimal("1000000"));
 * data.setHigh(new BigDecimal("180.00"));
 * data.setLow(new BigDecimal("177.50"));
 * data.setOpen(new BigDecimal("178.00"));
 * data.setTimestamp(LocalDateTime.now());
 * data.setIndicators(Map.of("MACD", new BigDecimal("0.12"), "RSI", new BigDecimal("65")));
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>indicators 字段建议使用统一命名规范，避免 key 拼写错误</li>
 *     <li>price、volume、high、low、open 建议使用 BigDecimal 避免浮点数误差</li>
 *     <li>timestamp 需要与行情数据源时间保持一致，注意时区</li>
 * </ul>
 *
 * Lombok 注解说明：
 * - @Data 提供 getter、setter、toString、equals、hashCode 方法
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
@Data
public class MarketData {
    /** 交易标的代码 */
    private String symbol;

    /** 当前价格 */
    private BigDecimal price;

    /** 成交量 */
    private BigDecimal volume;

    /** 当日最高价 */
    private BigDecimal high;

    /** 当日最低价 */
    private BigDecimal low;

    /** 当日开盘价 */
    private BigDecimal open;

    /** 数据时间戳 */
    private LocalDateTime timestamp;

    /** 技术指标集合，key 为指标名，value 为指标值 */
    private Map<String, Object> indicators;
}
