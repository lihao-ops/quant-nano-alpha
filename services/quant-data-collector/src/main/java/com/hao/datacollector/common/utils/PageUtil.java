package com.hao.datacollector.common.utils;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-01-15
 * @description: 分页工具类
 */
public class PageUtil {
    
    /**
     * 计算MySQL LIMIT的offset值
     * 
     * @param pageNo 页码（从1开始）
     * @param pageSize 每页大小
     * @return offset值
     */
    public static int calculateOffset(Integer pageNo, Integer pageSize) {
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        return (pageNo - 1) * pageSize;
    }
    
    /**
     * 获取默认页码
     * 
     * @param pageNo 页码
     * @return 处理后的页码
     */
    public static int getDefaultPageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }
    
    /**
     * 获取默认每页大小
     * 
     * @param pageSize 每页大小
     * @return 处理后的每页大小
     */
    public static int getDefaultPageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}