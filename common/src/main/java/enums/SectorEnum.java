package enums;

import lombok.Getter;

/**
 * 板块枚举类，定义了不同市场板块的相关信息
 *
 * @author: mliu.jeremy
 * @createTime: 2021/10/14
 * @description: 包含各类市场板块的代码、描述和类型信息
 */
@Getter
public enum SectorEnum {
    // 股票市场
    HS("a001010100000000", "全部A股", "A股"),
    BZ("1000038551000000", "北证A股", "京A"),
    B_SHARES("a001010600000000", "B股", "B股"),
    HK("1000040893000000", "全部港股（含并行代码）", "港股"),
    USA("1000022276000000", "全部上市美股(不含OTC)", "美股"),
    NEW_THIRD_BOARD("1000007748000000", "股转系统挂牌股票", "新三板"),
    
    // 债券相关
    CONVERTIBLE_BONDS("a101020600000000", "可转债", "可转债"),
    GOV_BONDS("a101020100000000", "国债", "国债"),
    CONVERTIBLES("1000019107000000", "转股类债券", "转股类债券"),
    
    // 指数相关
    HS_CORE_INDICATOR("a399010103000000", "沪深市场核心指数", "A股指数"),
    HK_CORE_INDICATOR("1000040976000000", "香港市场核心指数", "港股指数"),
    AMERICAS_CORE_INDICATOR("a39901010c000000", "美洲市场重要指数", "美股指数"),
    WIND_CONCEPT_INDEX("a39901012c000000", "万得概念指数", "万得概念指数"),
    WIND_FUND_MANAGER_INDEX("1000040925000000", "WIND基金经理业绩总指数", "基金经理指数"),
    WIND_FUND_COMPANY("1000040922000000", "WIND基金公司业绩指数", "基金公司指数"),
    
    // 基金相关
    ALL_ETF("1000019786000000", "全部上市基金", "基金");

    // 缓存所有枚举值，避免多次调用values()创建新数组
    private static final SectorEnum[] ALL_VALUES = values();
    
    /** 板块代码 */
    private final String code;
    
    /** 板块描述 */
    private final String desc;
    
    /** 板块类型 */
    private final String type;
    
    /** 板块代码参数（作为cloud参数） */
    private final String codeParam;

    /**
     * 构造函数
     *
     * @param code  板块代码
     * @param desc  板块描述
     * @param type  板块类型
     */
    SectorEnum(String code, String desc, String type) {
        this.code = code;
        this.desc = desc;
        this.type = type;
        this.codeParam = "macro=" + code;
    }

    /**
     * 获取所有板块代码
     *
     * @return 所有板块代码数组
     */
    public static String[] getAllCode() {
        String[] codes = new String[ALL_VALUES.length];
        for (int i = 0; i < ALL_VALUES.length; i++) {
            codes[i] = ALL_VALUES[i].getCode();
        }
        return codes;
    }

    /**
     * 根据类型获取板块枚举
     *
     * @param type 板块类型
     * @return 对应的板块枚举，若不存在则返回null
     */
    public static SectorEnum getByType(String type) {
        if (type == null) {
            return null;
        }
        
        for (SectorEnum item : ALL_VALUES) {
            if (item.type.equals(type)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 根据代码获取板块枚举
     *
     * @param code 板块代码
     * @return 对应的板块枚举，若不存在则返回null
     */
    public static SectorEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (SectorEnum item : ALL_VALUES) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}