package com.hao.datacollector.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-04 23:29:08
 * @description: 数学计算相关Util
 */
public class MathUtil {
    /**
     * 截取小数点后N位
     *
     * @param valueStr 待截取的数字字符串
     * @param digits   n位
     * @return 移动后N位数字
     */
    public static BigDecimal shiftDecimal(String valueStr, int digits) {
        BigDecimal raw = new BigDecimal(valueStr);

        if (digits >= 0) {
            BigDecimal divisor = BigDecimal.TEN.pow(digits);
            return raw.divide(divisor, 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal multiplier = BigDecimal.TEN.pow(-digits);
            return raw.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * 格式化数字精度（缩放并保留指定小数位数）
     * <p>
     * formatDecimal(14235, 1, true);   // 输出：1424.0 (14235/10=1423.5, 向上舍入到1424.0)
     * formatDecimal(14235, 1, false);  // 输出：1423.5 (14235/10=1423.5, 向下截断到1423.5)
     * formatDecimal(123456789, 5, true); // 输出：1235.0 (缩放后向上舍入)
     * formatDecimal(123456789, 5, false); // 输出：1234.56789 (缩放后向下截断)
     *
     * @param originalValue 原始值
     * @param decimalPlaces 保留小数位数
     * @param roundUp       是否向上舍入，true为向上舍入，false为向下截断
     * @return 格式化后的数值
     */
    public static double formatDecimal(Number originalValue, int decimalPlaces, boolean roundUp) {
        if (originalValue == null) return 0.0;
        BigDecimal value = new BigDecimal(originalValue.toString());

        // 这里根据需求除以10的decimalPlaces次方，缩放数字
        BigDecimal divisionFactor = BigDecimal.TEN.pow(decimalPlaces);
        value = value.divide(divisionFactor, decimalPlaces + 5, RoundingMode.HALF_UP);

        if (roundUp) {
            // 整数位四舍五入
            value = value.setScale(0, RoundingMode.HALF_UP);
            // 再保留小数位，向下截断（你也可以改成四舍五入）
            value = value.setScale(decimalPlaces, RoundingMode.DOWN);
        } else {
            // 不四舍五入，直接截断小数位
            value = value.setScale(decimalPlaces, RoundingMode.DOWN);
        }
        return value.doubleValue();
    }


    public static void main(String[] args) {
        System.out.println(formatDecimal(1453.5, 2, true));     // 应为 1424.0
        System.out.println(formatDecimal(1453.5, 2, false));    // 应为 1423.5
        System.out.println(formatDecimal(123456789, 5, true)); // 应为 1234.56790
        System.out.println(formatDecimal(123456789, 5, false));// 应为 1234.56789
    }

}

