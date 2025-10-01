package com.hao.strategyengine.service.interf;

import java.util.List;

/**
 * @author: hli
 * @program: quant-nano-alpha
 * @description: 装饰者模式实现:核心策略接口
 * @create: 2023-01-31 15:04
 **/
public interface StockStrategy {
    /**
     * 过滤方法
     *
     * @param list 需过滤的内容
     * @return 过滤后的内容
     */
    List<Object> filter(List<Object> list);
}
