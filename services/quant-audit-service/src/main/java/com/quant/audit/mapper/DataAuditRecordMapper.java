package com.quant.audit.mapper;

import com.quant.audit.model.DataAuditRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 数据审计记录Mapper接口
 */
@Mapper
public interface DataAuditRecordMapper {
    
    /**
     * 保存数据审计记录
     * 
     * @param record 数据审计记录
     * @return 影响行数
     */
    @Insert("INSERT INTO data_audit_record(service_name, table_name, operation_type, record_id, before_data, after_data, username, operation_time, create_time) " +
            "VALUES(#{serviceName}, #{tableName}, #{operationType}, #{recordId}, #{beforeData}, #{afterData}, #{username}, #{operationTime}, #{createTime})")
    int insert(DataAuditRecord record);
    
    /**
     * 根据条件查询数据审计记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param tableName 表名
     * @return 数据审计记录列表
     */
    @Select("<script>" +
            "SELECT * FROM data_audit_record WHERE 1=1 " +
            "<if test='startTime != null'> AND operation_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND operation_time &lt;= #{endTime} </if>" +
            "<if test='tableName != null and tableName != \"\"'> AND table_name = #{tableName} </if>" +
            "ORDER BY operation_time DESC" +
            "</script>")
    List<DataAuditRecord> findByCondition(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("tableName") String tableName);
    
    /**
     * 删除指定日期之前的数据审计记录
     * 
     * @param date 日期
     * @return 影响行数
     */
    @Select("DELETE FROM data_audit_record WHERE create_time < #{date}")
    int deleteByDateBefore(@Param("date") Date date);
}