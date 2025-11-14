package com.quant.data.archive.mapper;

import com.quant.data.archive.model.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface OperationLogMapper {
    
    /**
     * 保存操作日志
     * 
     * @param operationLog 操作日志
     * @return 影响行数
     */
    @Insert("INSERT INTO operation_log(service_name, operation_type, description, username, ip, request_url, method, params, result, execution_time, exception, create_time) " +
            "VALUES(#{serviceName}, #{operationType}, #{description}, #{username}, #{ip}, #{requestUrl}, #{method}, #{params}, #{result}, #{executionTime}, #{exception}, #{createTime})")
    int insert(OperationLog operationLog);
    
    /**
     * 根据条件查询操作日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    @Select("<script>" +
            "SELECT * FROM operation_log WHERE 1=1 " +
            "<if test='startTime != null'> AND create_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>" +
            "<if test='operationType != null and operationType != \"\"'> AND operation_type = #{operationType} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<OperationLog> findByCondition(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("operationType") String operationType);
    
    /**
     * 删除指定日期之前的日志
     * 
     * @param date 日期
     * @return 影响行数
     */
    @Select("DELETE FROM operation_log WHERE create_time < #{date}")
    int deleteByDateBefore(@Param("date") Date date);
}
