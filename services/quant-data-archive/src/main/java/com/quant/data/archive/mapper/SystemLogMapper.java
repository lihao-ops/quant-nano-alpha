package com.quant.data.archive.mapper;

import com.quant.data.archive.model.SystemLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 系统日志Mapper接口
 */
@Mapper
public interface SystemLogMapper {
    
    /**
     * 保存系统日志
     * 
     * @param systemLog 系统日志
     * @return 影响行数
     */
    @Insert("INSERT INTO system_log(level, service_name, operation_type, description, username, ip, request_url, method, params, result, execution_time, exception, create_time) " +
            "VALUES(#{level}, #{serviceName}, #{operationType}, #{description}, #{username}, #{ip}, #{requestUrl}, #{method}, #{params}, #{result}, #{executionTime}, #{exception}, #{createTime})")
    int insert(SystemLog systemLog);
    
    /**
     * 根据条件查询系统日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param level 日志级别
     * @return 系统日志列表
     */
    @Select("<script>" +
            "SELECT * FROM system_log WHERE 1=1 " +
            "<if test='startTime != null'> AND create_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time &lt;= #{endTime} </if>" +
            "<if test='level != null and level != \"\"'> AND level = #{level} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<SystemLog> findByCondition(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("level") String level);
    
    /**
     * 删除指定日期之前的日志
     * 
     * @param date 日期
     * @return 影响行数
     */
    @Select("DELETE FROM system_log WHERE create_time < #{date}")
    int deleteByDateBefore(@Param("date") Date date);
}
