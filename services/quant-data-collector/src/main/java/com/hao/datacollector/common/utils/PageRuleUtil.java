package com.hao.datacollector.common.utils;


import com.hao.datacollector.common.constant.CommonConstants;
import com.hao.datacollector.dto.PageNumDTO;

/**
 * @author LiHao
 * @description: 分页规则工具类
 * @Date 2023-09-13 10:55:18
 */
public class PageRuleUtil {
    /**
     * 获取分页页面计算方法
     * PageNumDTO pageParam = PageRuleUtil.getPageParam(pageNo, pageSize, CommonConstants.MYSQL_FLAG);
     * PageNumDTO pageParam = PageRuleUtil.getPageParam(pageNo, pageSize, CommonConstants.SQLSERVER_FLAG);
     * pageParam.getPageNo(), pageParam.getPageSize()
     *
     * @param pageNo   输入页码
     * @param pageSize 输入页数据量
     * @param dataType 1.MySQL,2.SqlServer
     * @return 分页数据传输对象
     */
    public static PageNumDTO getPageParam(Integer pageNo, Integer pageSize, Integer dataType) {
        PageNumDTO pageNumDTO = new PageNumDTO();
        if (CommonConstants.MYSQL_FLAG.equals(dataType)) {
            pageNumDTO.setPageNo((pageNo - 1) * pageSize);
            pageNumDTO.setPageSize(pageSize);
        } else if (CommonConstants.SQLSERVER_FLAG.equals(dataType)) {
            pageNumDTO.setPageNo((pageNo - 1) * pageSize);
            pageNumDTO.setPageSize(pageSize * pageNo);
        }
        return pageNumDTO;
    }
}