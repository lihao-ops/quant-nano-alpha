package enums;

import lombok.Getter;

/**
 * 云数据服务命令枚举
 * <p>
 * 维护了所有与 Wind 云数据服务交互时使用的命令及其说明。
 * 每个枚举实例代表一个具体的查询或操作。
 *
 * @author hli
 * @version 1.0
 * @since 2025-06-21
 */
@Getter
public enum CloudCommandEnum {

    /**
     * 获取所有A股股票列表（代码和名称）
     * <p>
     * - macro=a001010100000000: 指定宏为“全部A股”
     * - s_info_name: 获取股票名称
     * - tradeDate=s_trade_date(windcode,now(), 0): 获取最新交易日的日期
     */
    GET_ALL_A_STOCKS("WSS('macro=a001010100000000','s_info_name','tradeDate=s_trade_date(windcode,now(), 0)')", "获取整个A股市场的所有股票");

    /**
     * WSS 命令字符串
     */
    private final String command;

    /**
     * 命令功能说明
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param command     WSS 命令
     * @param description 命令说明
     */
    CloudCommandEnum(String command, String description) {
        this.command = command;
        this.description = description;
    }
}
