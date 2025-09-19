package dto;

import lombok.Data;

/**
 * @author LiHao
 * @description: 分页数据传输对象
 * @Date 2023-09-13 11:03:41
 */
@Data
//分页数据传输对象
public class PageNumDTO {
    //页码
    private Integer pageNo;

    //每页数量
    private Integer pageSize;
}