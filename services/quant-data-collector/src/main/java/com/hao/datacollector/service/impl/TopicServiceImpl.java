package com.hao.datacollector.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hao.datacollector.common.cache.StockCache;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.common.utils.PageRuleUtil;
import com.hao.datacollector.dal.dao.TopicMapper;
import com.hao.datacollector.dto.PageNumDTO;
import com.hao.datacollector.dto.kpl.CategoryLevel;
import com.hao.datacollector.dto.kpl.HotTopicKpl;
import com.hao.datacollector.dto.kpl.StockDetail;
import com.hao.datacollector.dto.kpl.TopicTable;
import com.hao.datacollector.dto.param.topic.TopicCategoryAndStockParam;
import com.hao.datacollector.dto.param.topic.TopicInfoParam;
import com.hao.datacollector.dto.param.topic.TopicStockQueryParam;
import com.hao.datacollector.dto.table.topic.InsertStockCategoryMappingDTO;
import com.hao.datacollector.dto.table.topic.InsertTopicCategoryDTO;
import com.hao.datacollector.dto.table.topic.InsertTopicInfoDTO;
import com.hao.datacollector.dto.table.topic.TopicStockDTO;
import com.hao.datacollector.service.StockProfileService;
import com.hao.datacollector.service.TopicService;
import com.hao.datacollector.web.vo.stockProfile.SearchKeyBoardVO;
import com.hao.datacollector.web.vo.topic.TopicCategoryAndStockVO;
import com.hao.datacollector.web.vo.topic.TopicInfoKplVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author Hao Li
 * @Date 2025-07-22 10:50:08
 * @description: 题材实现类
 */
@Slf4j
@Service
public class TopicServiceImpl implements TopicService {

    @Value("${kpl.topic.url}")
    private String kplTopicUrl;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private StockProfileService stockProfileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 转档题材库
     *
     * @param startId 遍历题材起始id
     * @param endId   遍历题材结束id
     * @return 转档结果
     */
    @Override
    public Boolean setKplTopicInfoJob(Integer startId, Integer endId) {
        for (int id = startId; id <= endId; id++) {
            String kplTopicDataStr = getRequestKplTopicData(id);
            // 解析JSON为对象
            HotTopicKpl hotTopic = null;
            try {
                hotTopic = objectMapper.readValue(kplTopicDataStr, HotTopicKpl.class);
            } catch (Exception e) {
                log.error("setKplTopicInfoJob_convertData_error,id={},result={}", id, kplTopicDataStr);
                continue;
//                throw new RuntimeException("setKplTopicInfoJob_convertData_error!");
            }
            if (hotTopic == null || !StringUtils.hasLength(hotTopic.getId())) {
                log.error("setKplTopicInfoJob_getKplTopicData_data_error,id={},result={}", id, kplTopicDataStr);
                continue;
            }
            //转换插入
            Boolean insertResult = insertKplTopicInsertData(hotTopic);
            log.info("setKplTopicInfoJob_ID={},result={}", id, insertResult);
        }
        return true;
    }

    /**
     * 获取KPL主题话题数据
     *
     * @param id 主题话题ID参数
     * @return HTTP响应的字符串结果
     */
    private String getRequestKplTopicData(Integer id) {
        // 构造HTTP请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "lhb/5.20.7 (com.kaipanla.www; build:0; iOS 16.2.0) Alamofire/4.9.1");
        // 构造请求体参数
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("DeviceID", "26a33d6b656c5a0f8fe859414b5daa0a877e3cb3");
        body.add("ID", String.valueOf(id));
//        body.add("ID", String.valueOf(20)); // 注意：这里写死了ID为25，没有使用传入参数
        body.add("PhoneOSNew", "2");
        body.add("Token", "31835bf8e1ff2ac1c5b1001195e0f138");
        body.add("UserID", "4239370");
        body.add("VerSion", "5.20.0.7");
        body.add("a", "InfoGet");
        body.add("apiv", "w41");
        body.add("c", "Theme");
        ResponseEntity<String> response = HttpUtil.sendRequestFormPost(
                kplTopicUrl,
                body,
                headers,
                3000, // connectTimeout ms
                5000  // readTimeout ms
        );
        // 检查响应状态码，非2xx状态码抛出异常
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("setKplTopicInfoJob_getRequestKplTopicData_error,result=" + response.getStatusCode());
        }
        // 记录响应数据大小
        log.info("setKplTopicInfoJob_response.size={}", response.getBody().length());
        return response.getBody();
    }

    /**
     * 插入题材相关数据
     *
     * @param hotTopic 题材对象
     * @return 插入结果
     */
    private Boolean insertKplTopicInsertData(HotTopicKpl hotTopic) {
        log.info("insertKplTopicInsertData_start_processing_topic_data,topicId={}", hotTopic.getId());
        //先转换
        InsertTopicInfoDTO insertTopicInfoDTO = new InsertTopicInfoDTO();
        BeanUtils.copyProperties(hotTopic, insertTopicInfoDTO);
        insertTopicInfoDTO.setTopicId(Integer.valueOf(hotTopic.getId()));
        //主题时间
        insertTopicInfoDTO.setTopicCreateTime(DateUtil.timestampToDateStr(Long.parseLong(hotTopic.getCreateTime())));
        insertTopicInfoDTO.setTopicUpdateTime(DateUtil.timestampToDateStr(Long.parseLong(hotTopic.getUpdateTime())));
        if (StringUtils.hasLength(hotTopic.getFirstShelveTime())) {
            insertTopicInfoDTO.setFirstShelveTime(DateUtil.timestampToDateStr(Long.parseLong(hotTopic.getFirstShelveTime())));
        }
        if (StringUtils.hasLength(hotTopic.getUpdateCacheTime())) {
            insertTopicInfoDTO.setUpdateCacheTime(DateUtil.timestampToDateStr(Long.parseLong(hotTopic.getUpdateCacheTime())));
        }
        //类别信息列表
        List<InsertTopicCategoryDTO> insertCategoryList = new ArrayList<>();
        //类别所属股票映射信息列表
        List<InsertStockCategoryMappingDTO> insertStockCategoryMappingList = new ArrayList<>();
        List<TopicTable> categoryList = hotTopic.getTable();
        if (categoryList == null || categoryList.isEmpty()) {
            log.warn("insertKplTopicInsertData_category_list_is_empty,_topicId={}", hotTopic.getId());
            return false;
        }
        log.info("insertKplTopicInsertData_start_processing_categories,topicId={},category_count={}", hotTopic.getId(), categoryList.size());
        for (TopicTable category : categoryList) {
            InsertTopicCategoryDTO insertCategoryLevel1 = new InsertTopicCategoryDTO();
            CategoryLevel level1 = category.getLevel1();
            BeanUtils.copyProperties(level1, insertCategoryLevel1);
            insertCategoryLevel1.setTopicId(Integer.valueOf(hotTopic.getId()));
            //一级类别数据parentId = 0
            insertCategoryLevel1.setParentId(0);
            insertCategoryLevel1.setCategoryId(Integer.valueOf(level1.getId()));
            if (StringUtils.hasLength(level1.getFirstShelveTime())) {
                insertCategoryLevel1.setFirstShelveTime(DateUtil.timestampToDateStr(Long.parseLong(level1.getFirstShelveTime())));
            }
            if (StringUtils.hasLength(level1.getUpdateCacheTime())) {
                insertCategoryLevel1.setUpdateCacheTime(DateUtil.timestampToDateStr(Long.parseLong(level1.getUpdateCacheTime())));
            }
            insertCategoryList.add(insertCategoryLevel1);
            //一级类别股票映射信息
            List<StockDetail> level1Stocks = level1.getStocks();
            if (level1Stocks != null) {
                for (StockDetail level1Stock : level1Stocks) {
                    InsertStockCategoryMappingDTO stockMappingDTO = new InsertStockCategoryMappingDTO();
                    BeanUtils.copyProperties(level1Stock, stockMappingDTO);
                    stockMappingDTO.setWindCode(getWindCodeMapping(level1Stock.getStockId()));
                    String windName = getWindName(stockMappingDTO.getWindCode());
                    stockMappingDTO.setWindName(windName);
                    stockMappingDTO.setCategoryId(insertCategoryLevel1.getCategoryId());
                    if (StringUtils.hasLength(level1Stock.getFirstShelveTime())) {
                        stockMappingDTO.setFirstShelveTime(DateUtil.timestampToDateStr(Long.parseLong(level1Stock.getFirstShelveTime())));
                    }
                    if (StringUtils.hasLength(level1Stock.getUpdateCacheTime())) {
                        stockMappingDTO.setUpdateCacheTime(DateUtil.timestampToDateStr(Long.parseLong(level1Stock.getUpdateCacheTime())));
                    }
                    insertStockCategoryMappingList.add(stockMappingDTO);
                }
            }
            List<CategoryLevel> level2List = category.getLevel2();
            if (level2List != null) {
                for (CategoryLevel level2 : level2List) {
                    InsertTopicCategoryDTO insertCategoryLevel2 = new InsertTopicCategoryDTO();
                    BeanUtils.copyProperties(level2, insertCategoryLevel2);
                    //时间戳转换
                    if (StringUtils.hasLength(level2.getFirstShelveTime())) {
                        insertCategoryLevel2.setFirstShelveTime(DateUtil.timestampToDateStr(Long.parseLong(level2.getFirstShelveTime())));
                    }
                    if (StringUtils.hasLength(level2.getUpdateCacheTime())) {
                        insertCategoryLevel2.setUpdateCacheTime(DateUtil.timestampToDateStr(Long.parseLong(level2.getUpdateCacheTime())));
                    }
                    insertCategoryLevel2.setTopicId(Integer.valueOf(hotTopic.getId()));
                    insertCategoryLevel2.setCategoryId(Integer.valueOf(level2.getId()));
                    //指定一级类别id为父id
                    insertCategoryLevel2.setParentId(insertCategoryLevel1.getCategoryId());
                    insertCategoryList.add(insertCategoryLevel2);

                    //二级类别股票映射信息
                    List<StockDetail> level2Stocks = level2.getStocks();
                    for (StockDetail level2Stock : level2Stocks) {
                        InsertStockCategoryMappingDTO stockMappingDTO = new InsertStockCategoryMappingDTO();
                        BeanUtils.copyProperties(level2Stock, stockMappingDTO);
                        stockMappingDTO.setWindCode(getWindCodeMapping(level2Stock.getStockId()));
                        //存在匹配不到windCode的股票，特殊处理
                        String windName = getWindName(stockMappingDTO.getWindCode());
                        stockMappingDTO.setWindName(windName);
                        stockMappingDTO.setCategoryId(insertCategoryLevel2.getCategoryId());
                        //时间戳转换
                        if (StringUtils.hasLength(level2Stock.getFirstShelveTime())) {
                            stockMappingDTO.setFirstShelveTime(DateUtil.timestampToDateStr(Long.parseLong(level2Stock.getFirstShelveTime())));
                        }
                        if (StringUtils.hasLength(level2Stock.getUpdateCacheTime())) {
                            stockMappingDTO.setUpdateCacheTime(DateUtil.timestampToDateStr(Long.parseLong(level2Stock.getUpdateCacheTime())));
                        }
                        insertStockCategoryMappingList.add(stockMappingDTO);
                    }
                }
            }
        }
        log.info("insertKplTopicInsertData_data_processing_completed,topicId={},_total_categories={},total_stock_mappings={}", hotTopic.getId(), insertCategoryList.size(), insertStockCategoryMappingList.size());
        List<InsertTopicInfoDTO> insertTopicInfoList = new ArrayList();
        insertTopicInfoList.add(insertTopicInfoDTO);
        return insertTopicInfo(insertTopicInfoList, insertCategoryList, insertStockCategoryMappingList);
    }

    /**
     * 插入转档题材相关数据kpl
     *
     * @param insertTopicInfoList            题材信息list
     * @param insertCategoryList             类别信息list
     * @param insertStockCategoryMappingList 股票映射信息list
     * @return 操作结果
     */
    private Boolean insertTopicInfo
    (List<InsertTopicInfoDTO> insertTopicInfoList, List<InsertTopicCategoryDTO> insertCategoryList, List<InsertStockCategoryMappingDTO> insertStockCategoryMappingList) {
        int insertTopicNum = 0, insertCategoryNum = 0, insertStockNum = 0;
        if (!insertTopicInfoList.isEmpty()) {
            insertTopicNum = topicMapper.insertTopicInfoList(insertTopicInfoList);
        }
        if (!insertCategoryList.isEmpty()) {
            insertCategoryNum = topicMapper.insertCategoryList(insertCategoryList);
        }
        if (!insertStockCategoryMappingList.isEmpty()) {
            insertStockNum = topicMapper.insertStockCategoryMappingList(insertStockCategoryMappingList);
        }
        log.info("insertTopicInfo_insertTopicNum={},insertCategoryNum={},insertStockNum={}", insertTopicNum, insertCategoryNum, insertStockNum);
        return insertTopicNum + insertCategoryNum + insertStockNum > 0;
    }

    /**
     * 获取映射带有后缀的股票代码
     *
     * @param stockId 未带后缀股票代码
     * @return 带有后缀股票代码
     */
    private String getWindCodeMapping(String stockId) {
        //匹配股票代码后缀
        String windCode = StockCache.getWindCodeByStockId(stockId);
        if (StringUtils.hasLength(windCode)) {
            return windCode;
        }
        //匹配不到则调用键盘精灵接口获取,实在获取不到则返回原始不带后缀股票代码
        List<SearchKeyBoardVO> searchKeyBoard = stockProfileService.getSearchKeyBoard(stockId, 1, 10);
        if (searchKeyBoard != null && searchKeyBoard.size() > 0) {
            return searchKeyBoard.get(0).getWindCode();
        }
        return stockId;
    }

    /**
     * 获取股票名称
     *
     * @param windCode 股票代码
     * @return 对应的股票名称
     */
    private String getWindName(String windCode) {
        String windName = StockCache.getWindNameByWindCode(windCode);
        if (StringUtils.hasLength(windName)) {
            return windName;
        }
        //匹配不到则调用键盘精灵接口获取,实在获取不到则返回原始不带后缀股票代码
        List<SearchKeyBoardVO> searchKeyBoard = stockProfileService.getSearchKeyBoard(windCode, 1, 10);
        if (searchKeyBoard != null && searchKeyBoard.size() > 0) {
            return searchKeyBoard.get(0).getName();
        }
        log.error("getWindName_error!_windCode={},windName={}", windCode, windName);
        return Strings.EMPTY;
    }

    /**
     * 获取热门题材信息列表
     *
     * @param queryDTO 题材信息查询参数对象，包含分页、筛选、排序等条件
     * @return 题材信息列表
     */
    @Override
    public List<TopicInfoKplVO> getKplTopicInfoList(TopicInfoParam queryDTO) {
        // 获取分页参数
        PageNumDTO pageParam = PageRuleUtil.getPageParam(queryDTO.getPageNo(), queryDTO.getPageSize(), 1);
        queryDTO.setPageNo(pageParam.getPageNo());
        queryDTO.setPageSize(pageParam.getPageSize());
        return topicMapper.getKplTopicInfoList(queryDTO);
    }

    /**
     * 获取题材分类及股票映射列表
     *
     * @param queryDTO 题材分类及股票映射查询参数对象，包含分页、筛选、排序等条件
     * @return 题材分类及股票映射列表
     */
    @Override
    public List<TopicCategoryAndStockVO> getKplCategoryAndStockList(TopicCategoryAndStockParam queryDTO) {
        // 获取分页参数
        PageNumDTO pageParam = PageRuleUtil.getPageParam(queryDTO.getPageNo(), queryDTO.getPageSize(), 1);
        queryDTO.setPageNo(pageParam.getPageNo());
        queryDTO.setPageSize(pageParam.getPageSize());
        return topicMapper.getKplCategoryAndStockList(queryDTO);
    }

    /**
     * 获取题材Id映射股票列表
     *
     * @param query 题材Id映射股票查询参数对象
     * @return Map<topic_id, List < wind_code, wind_name>>
     */
    @Override
    public Map<Integer, List<TopicStockDTO>> getKplTopicAndStockList(TopicStockQueryParam query) {
        List<Integer> kplAllTopicIdList = new LinkedList<>();
        //topicId = null则查询所有
        if (query == null || query.getTopicId() == null) {
            kplAllTopicIdList = topicMapper.getKplAllTopicIdList();
        } else {
            kplAllTopicIdList.add(query.getTopicId());
        }
        List<TopicStockDTO> kplTopicAndStockList = topicMapper.getKplTopicAndStockList(kplAllTopicIdList);
        Map<Integer, List<TopicStockDTO>> resultMap = new HashMap<>();
        for (TopicStockDTO dto : kplTopicAndStockList) {
            resultMap.computeIfAbsent(dto.getTopicId(), k -> new ArrayList<>()).add(dto);
        }
        return resultMap;
    }
}