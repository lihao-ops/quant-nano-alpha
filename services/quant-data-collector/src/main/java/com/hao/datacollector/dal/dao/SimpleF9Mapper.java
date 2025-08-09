package com.hao.datacollector.dal.dao;

import com.hao.datacollector.dto.table.f9.InsertCompanyProfileDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SimpleF9Mapper {
    /**
     * 插入公司简介转档数据
     *
     * @param insertCompanyProfileDTO 转档对象
     * @return 操作结果
     */
    int batchInsertCompanyProfileDataJob(@Param("param") List<InsertCompanyProfileDTO> insertCompanyProfileDTO);

    /**
     * 获取已转档公司简介的windCode
     *
     * @return 已转档windCode列表
     */
    List<String> getInsertFinancialSummaryData();
}
