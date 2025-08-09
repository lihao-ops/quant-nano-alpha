package com.hao.datacollector.dto.param.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TopicDetailParam {
    @Schema(description = "日期 格式：20190214", required = true)
    private String dateTime;

    @Schema(description = "股票过滤列表:1:只看低价：股价<10元, 2: 只看小盘：流通市值<20亿,3: 主力流入: 流入金额>1千万,4:是否剔除ST: 剔除ST", required = true)
    private String stockFilterIds = "4";

    @Schema(description = "功能选项:1:涨停、2：快涨停，3：集合竞价开板，4：涨速：5：主力：6开板", required = true)
    private int functionId = 1;

    @Schema(description = "排序字段ID:2：涨停时间、 3：状态、 4:股票热度、 5涨停封单额，6价格，7主力流入，8涨幅，9开板时间，10换手率，11开板次数，12流通市值,13涨停价委买,14: 5分钟涨跌幅,15:1分钟涨跌幅，16：5分钟涨速，17：1分钟涨速 18：主力强度，19：主力成本，20：主力浮盈，21：主力流入，22：主力分歧，23：主力时间，24：主力撤买，25：主力撤卖，26：主力买入（次)，27：主力卖出（次）", required = true)
    private int sortFieldId = 2;

    @Schema(description = "排序方式：1降序，2升序。注意：当本字段为0时，则按默认方式排序，即恢复最初系统定的sortFieldId和sortType的取值。", required = true)
    private int sortType = 1;

    @Schema(description = "标签ID列表:可以选择多个。含有-1表示查所有，999表示其他。-1：全部", required = true)
    private String topicIds;

    @Schema(description = "当前页码", required = false)
    private int currentPage;

    @Schema(description = "每页大小", required = false)
    private int pageSize;
}