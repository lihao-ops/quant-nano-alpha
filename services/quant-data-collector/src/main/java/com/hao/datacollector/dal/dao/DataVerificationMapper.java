package com.hao.datacollector.dal.dao;

import com.hao.datacollector.dto.table.verification.QuotationVerificationDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author hli
 * @description 数据校验专用Mapper
 */
public interface DataVerificationMapper {

    /**
     * 统计表总行数（动态表名）
     */
    Long countTable(@Param("tableName") String tableName);

    /**
     * 统计目标表指定时间范围内的数据量（动态表名）
     */
    Long countTargetByRange(@Param("tableName") String tableName,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate);

    /**
     * 源表 Keyset Paging 分页查询
     */
    List<QuotationVerificationDTO> fetchSourceBatch(@Param("tableName") String tableName,
                                                    @Param("lastCode") String lastCode,
                                                    @Param("lastDate") String lastDate,
                                                    @Param("batchSize") int batchSize);

    /**
     * 目标表范围精准查询
     */
    List<QuotationVerificationDTO> fetchTargetBatchInScope(@Param("tableName") String tableName,
                                                           @Param("startDate") String startDate,
                                                           @Param("endDate") String endDate,
                                                           @Param("firstCode") String firstCode,
                                                           @Param("firstDate") String firstDate,
                                                           @Param("lastCode") String lastCode,
                                                           @Param("lastDate") String lastDate);
}