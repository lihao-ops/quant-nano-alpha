package com.hao.datacollector.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hao.datacollector.common.utils.HttpUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Hao Li
 * @program: DataShareService
 * @Date 2025-07-17 17:54:34
 * @description:
 */
@Data
@Schema(description = "热门主题响应DTO")
class HotTopicResponse {
    @JsonProperty("ID")
    @Schema(description = "主题ID", example = "22")
    private String id;

    @JsonProperty("Name")
    @Schema(description = "主题名称", example = "BC电池")
    private String name;

    @JsonProperty("BriefIntro")
    @Schema(description = "简介", example = "BC电池是将PN结和金属接触都设于太阳电池背面...")
    private String briefIntro;

    @JsonProperty("ClassLayer")
    @Schema(description = "分类层级", example = "3")
    private String classLayer;

    @JsonProperty("Desc")
    @Schema(description = "描述")
    private String desc;

    @JsonProperty("PlateSwitch")
    @Schema(description = "板块开关", example = "2")
    private String plateSwitch;

    @JsonProperty("StkSwitch")
    @Schema(description = "股票开关", example = "2")
    private String stkSwitch;

    @JsonProperty("Introduction")
    @Schema(description = "详细介绍")
    private String introduction;

    @JsonProperty("CreateTime")
    @Schema(description = "创建时间")
    private String createTime;

    @JsonProperty("UpdateTime")
    @Schema(description = "更新时间")
    private String updateTime;

    @JsonProperty("Table")
    @Schema(description = "主题分类表")
    private List<TopicTable> table;

    @JsonProperty("Stocks")
    @Schema(description = "股票列表")
    private List<Object> stocks;

    @JsonProperty("StockList")
    @Schema(description = "股票信息列表")
    private List<StockInfo> stockList;

    @JsonProperty("IsNew")
    @Schema(description = "是否新增", example = "1")
    private Integer isNew;

    @JsonProperty("Power")
    @Schema(description = "权重", example = "100")
    private Integer power;

    @JsonProperty("Subscribe")
    @Schema(description = "订阅数", example = "1000")
    private Integer subscribe;

    @JsonProperty("ZT")
    @Schema(description = "涨停相关数据")
    private Map<String, List<Object>> zt;

    @JsonProperty("IsGood")
    @Schema(description = "是否点赞", example = "1")
    private Integer isGood;

    @JsonProperty("GoodNum")
    @Schema(description = "点赞数", example = "48")
    private Integer goodNum;

    @JsonProperty("ComNum")
    @Schema(description = "评论数", example = "483")
    private Integer comNum;

    @Schema(description = "错误码")
    private String errcode;

    @Schema(description = "时间戳")
    private Double t;
}

@Data
@Schema(description = "主题分类表DTO")
class TopicTable {
    @JsonProperty("Level1")
    @Schema(description = "一级分类")
    private CategoryLevel level1;

    @JsonProperty("Level2")
    @Schema(description = "二级分类列表")
    private List<CategoryLevel> level2;
}

@Data
@Schema(description = "分类级别DTO")
class CategoryLevel {
    @JsonProperty("ID")
    @Schema(description = "分类ID", example = "1534")
    private String id;

    @JsonProperty("Name")
    @Schema(description = "分类名称", example = "材料")
    private String name;

    @JsonProperty("ZSCode")
    @Schema(description = "指数代码")
    private String zsCode;

    @JsonProperty("Stocks")
    @Schema(description = "股票详情列表")
    private List<StockDetail> stocks;

    @JsonProperty("IsNew")
    @Schema(description = "是否新增", example = "1")
    private Integer isNew;
}

@Data
@Schema(description = "股票详情DTO")
class StockDetail {
    @JsonProperty("StockID")
    @Schema(description = "股票ID", example = "300537")
    private String stockId;

    @JsonProperty("IsZz")
    @Schema(description = "是否主做", example = "1")
    private String isZz;

    @JsonProperty("IsHot")
    @Schema(description = "是否热门", example = "1")
    private String isHot;

    @JsonProperty("Reason")
    @Schema(description = "入选原因", example = "光伏板块的BC电池用光伏绝缘胶开始逐渐放量")
    private String reason;

    @JsonProperty("IsNew")
    @Schema(description = "是否新增", example = "1")
    private Integer isNew;

    @JsonProperty("prod_name")
    @Schema(description = "产品名称", example = "广信材料")
    private String prodName;

    @JsonProperty("Hot")
    @Schema(description = "热度值", example = "999")
    private Integer hot;
}

@Data
@Schema(description = "股票信息DTO")
class StockInfo {
    @JsonProperty("StockID")
    @Schema(description = "股票ID", example = "601012")
    private String stockId;

    @JsonProperty("Tag")
    @Schema(description = "股票标签列表")
    private List<StockTag> tag;

    @JsonProperty("prod_name")
    @Schema(description = "股票名称", example = "隆基绿能")
    private String prodName;

    @JsonProperty("HotNum")
    @Schema(description = "热度数值", example = "1053")
    private Integer hotNum;
}

@Data
@Schema(description = "股票标签DTO")
class StockTag {
    @JsonProperty("ID")
    @Schema(description = "标签ID", example = "1532")
    private String id;

    @JsonProperty("Name")
    @Schema(description = "标签名称", example = "电池组件")
    private String name;

    @JsonProperty("Reason")
    @Schema(description = "标签原因", example = "BC产能主要为30GW HPBC电池项目")
    private String reason;
}

// 测试类
class test {
    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String URL = "https://applhb.longhuvip.com/w1/api/index.php";

    @Test
    void getRequest() {
        for (int id = 1; id <= 1000; id++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("User-Agent", "lhb/5.20.7 (com.kaipanla.www; build:0; iOS 16.2.0) Alamofire/4.9.1");
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("DeviceID", "26a33d6b656c5a0f8fe859414b5daa0a877e3cb3");
//                body.add("ID", String.valueOf(id));
                body.add("ID", String.valueOf(25));
                body.add("PhoneOSNew", "2");
                body.add("Token", "31835bf8e1ff2ac1c5b1001195e0f138");
                body.add("UserID", "4239370");
                body.add("VerSion", "5.20.0.7");
                body.add("a", "InfoGet");
                body.add("apiv", "w41");
                body.add("c", "Theme");

                ResponseEntity<String> response = HttpUtil.sendRequestFormPost(
                        URL,
                        body,
                        headers,
                        3000, // connectTimeout ms
                        5000  // readTimeout ms
                );
                if (response.getStatusCode().is2xxSuccessful()) {
                    String result = response.getBody();

                    if (result != null && !result.isBlank()) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(result);
                        // 判断几个字段是否都是空数组
                        boolean noData = isEmptyArray(root, "Table") &&
                                isEmptyArray(root, "Stocks") &&
                                isEmptyArray(root, "StockList") &&
                                !root.has("ID");

                        if (noData) {
                            System.out.printf("ID=%d 响应成功但无业务数据，跳过%n", id);
                            continue;
                        }
                        // 有效数据才处理
                        System.out.printf("ID=%d 响应成功，有效数据，响应长度=%d%n", id, result.length());
                        getData(response.getBody());
                        // 你可以在此处存库
                        // ThemeInfo info = ...
                        // repository.save(info);
                    }
                } else {
                    System.out.printf("ID=%d 响应失败，状态码=%s%n", id, response.getStatusCode());
                }
                Thread.sleep(300); // 建议限速防止被ban
            } catch (Exception e) {
                System.err.printf("请求ID=%d失败: %s%n", id, e.getMessage());
            }
        }
    }

    private static boolean isEmptyArray(JsonNode node, String field) {
        return node.has(field) && node.get(field).isArray() && node.get(field).size() == 0;
    }

    public static void getData(String jsonData) {
        if (!StringUtils.hasLength(jsonData)) {
            jsonData = "{\n" +
                    "\t\"ID\": \"22\",\n" +
                    "\t\"Name\": \"BC电池\",\n" +
                    "\t\"BriefIntro\": \"BC电池是将PN结和金属接触都设于太阳电池背面，电池片正面采用SiNx/SiOx双层减反钝化薄膜，没有金属电极遮挡，最大限度地利用入射光，减少光学损失，带来更多有效发电面积，拥有高转换效率，且外观上更加美观。\",\n" +
                    "\t\"ClassLayer\": \"3\",\n" +
                    "\t\"Desc\": \"\",\n" +
                    "\t\"PlateSwitch\": \"2\",\n" +
                    "\t\"StkSwitch\": \"2\",\n" +
                    "\t\"Introduction\": \"<p><strong>题材相关新闻:</strong></p><p>12月7日隆基绿能、创维光伏和交银金租签约仪式在西安隆基绿能总部举行。本次签约标志着全国知名光伏平台公司—创维光伏将在工商业项目中大规模应用隆基Hi-MO X6极智家防积灰设计组件，双方将共同致力于打造“创维光伏&amp;隆基防积灰组件”的工商业系统解决方案，为业主提供品质电站。</p><p>10月8日隆基绿能Hi-MO X6防积灰组件发布，在具备Hi-MO X6产品兼具美观、发电高效、安全可靠等核心价值的基础上，新增了防积灰功能</p><p>2023年9月5号</p><p>隆基绿能在今日进行的半年度业绩电话会上表示确定BC电池为主要技术路线，相信它会逐步取代TOPCon。</p><p><strong>题材基本面介绍:</strong></p><p>BC技术作为平台技术，可兼容HJT(HBC) 、TOPCon(TBC) 、PERC (HPBC)IBC(Interdigitated back contact,交叉背接触)电池技术则是另一种思路，它将电池正面的电极栅线全部转移到电池背面，通过减少栅线对阳光的遮挡来提高转换效率。</p><p>这意味着IBC可以与PERC、TOPCon、HJT、钙钛矿等多种技术叠加来进一步提升转化效率，因此有望成为新一代的平台型技术，组合应用潜力巨大。</p><p>IBC叠加到TOPCon上，叫TBC电池，转换效率能到25%-26%;<br/></p><p>IBC叠加到HJT上，叫HBC电池，转换效率能到26%-27%。</p><p>IBC电池有着高效、灵活、美观的优势。</p><p>这种结构使得IBC电池相比传统晶硅电池具有三个显著的优势:</p><p>(1)电池正面无栅线遮挡，避免了金属电极遮光损失，显著提升电池转换效率，是目前实验室效率最高的晶硅电池;</p><p>(2)正负电极均位于背面，工艺流程更加灵活;</p><p>(3) 外形美观，商业化前景好，完美契合光伏建筑一体化。</p><p>BC电池的技术概况</p><p>BC电池的制造工艺相对复杂，需要多重步骤和高精度操作。核心工艺包括激光图形化、隔离、金属化和钝化，对设备和技术有高要求。此外，BC电池的材料需求高，要求少子寿命相对较高，制造过程中需要用到高质量的单晶硅片。这些技术门槛和成本因素限制了BC电池的大规模商业化进展。</p><p><img src=\\\"https://appresi.longhuvip.com/uploadImg/xuetang/article/202312/1703581907765014.png\\\" title=\\\"1703581739482994.png\\\" alt=\\\"image.png\\\"/></p><p><img src=\\\"https://appresi.longhuvip.com/uploadImg/xuetang/article/202312/1703581907144809.png\\\" title=\\\"1703581752980341.png\\\" alt=\\\"image.png\\\"/></p><p>BC电池的技术路线包括HPBC、TBC和HBC等，其中HPBC技术已经开始规模化扩产。不同技术路线的成熟度存在差异，影响着BC电池的市场竞争力与前景。</p><p>BV电池应用空间广阔</p><p>BV电池应用场景丰富，天然适用于分布式光伏场景。由于 BC 电池正面无栅线，外表更美观，更契合分布式户用场景需求，特别是 BIPV，可以充分结合 BC 组件的美观和建筑艺术，做到光伏建筑一体化。以海外主要市场——欧洲为例，欧洲大部分新增装机量都以分布式为主，且欧洲更喜欢黑色屋顶的建筑风格，十分契合 BC 电池的特点。近年来国内外分布式装机量增速明显高于集中式，2022 年，国内分布式新增装机量占比 58.47%。</p><p><img src=\\\"https://appresi.longhuvip.com/uploadImg/xuetang/article/202312/1703581907131641.png\\\" title=\\\"1703581799674506.png\\\" alt=\\\"image.png\\\"/></p><p>由于 BC 电池可以做成柔性组件，具有重量轻、可弯曲的特点，多应用于车辆、轮船、卫星以及承重能力较差的建筑物上。</p><p>国内BC电池市场竞争格局</p><p>BC电池产业链包括上游的硅料和硅片，中游的电池片和组件，以及下游的光伏电站，同时涵盖光伏设备和辅料。产业链中，光伏设备和辅料有望迎来增长，BC电池的广泛应用将推动激光设备、POE胶膜等材料需求上升。</p><p>据《全球光伏》每月汇总的TaiyangNews组件最高效率排名来看，BC技术也经常排在首位。效率方面，隆基的BC电池平均效率也达到了25%以上，爱旭的ABC电池平均效率也达到了26.5%。</p><p><img src=\\\"https://appresi.longhuvip.com/uploadImg/xuetang/article/202312/1703581907416310.png\\\" title=\\\"1703581833532337.png\\\" alt=\\\"image.png\\\"/></p><p>BC技术的优点除了单面转化率增加之外，也比较适合BIPV光伏建筑一体化。这些场景本身也没办法利用电池板背面，使得双面电池无用武之处；另外，BC电池也更美观，和建筑可以更好契合。</p><p>国内BC电池龙头企业</p><p>中国BC电池领域的主要企业包括隆基绿能、爱旭股份、晶澳科技、宇邦新材、捷佳伟创等。目前隆基绿能已投产HPBC电池，计划建设12GW高效单晶电池项目。爱旭股份采用ABC电池技术，计划布局52GW产能，预计平均效率可达25.5%以上。晶澳科技致力于研究IBC、钙钛矿和叠层电池技术。宇邦新材供应BC电池隔膜，最近研发的多层复合隔膜提升电池性能。捷佳伟创在多种电池技术路线上布局，成为BC设备龙头。其他光伏厂商如晶科能源、TCL中环、中来股份也积极跟进BC电池领域，展现出强大的技术创新和市场竞争力。</p><p><img src=\\\"https://appresi.longhuvip.com/uploadImg/xuetang/article/202312/1703581907769883.png\\\" title=\\\"1703581864266128.png\\\" alt=\\\"image.png\\\"/></p><p>隆基绿能：光伏行业头部企业，BC 电池的领导者</p><p>隆基绿能业务涵盖光伏全产业链，盈利能力不断增强。隆基绿能是头部光伏一体化企业，专注于为全球客户提供高效单晶硅太阳能发电解决方案。公司业务涵盖单晶硅棒、硅片、电池和组件的研发、生产制造和销售，提供电站的开发和系统解决方案。2018-2022 年受益于光伏需求爆发，公司 4 年间营业收入 CAGR55.63%，2023 年前三季度实现营收 941 亿元，同比增长 8.55%，实现利润 116.48 亿元，同比增长 6.4%。分产品来看，太阳能组件、硅片和硅棒贡献绝大部分收入。&nbsp;</p><p>目前虽然光伏行业尚处于产能过剩的下行周期， 但在政策和产业经济性选择的推动下，我们认为低端产能将逐渐被出清，市场集中度逐渐 增加。在强者更强的背景下，企业要有足够的能力才能撑过“退潮期”，而隆基三大优势 将持续巩固其头部企业地位。</p><p><img src=\\\"https://appresi.longhuvip.com/uploadImg/xuetang/article/202312/1703581907845193.png\\\" title=\\\"1703581891261103.png\\\" alt=\\\"image.png\\\"/></p><p>随着下游应用市场不断拓展，叠加龙头企业引领作用及产业链配套全面发展，BC电池将迎来快速发展和普及，BC电池市场份额将迎来快速提升，预计2023年BC电池的市场份额有望突破3%，2024年有望达到8%，到2025年或达到15%以上。</p>\",\n" +
                    "\t\"CreateTime\": \"1699604590\",\n" +
                    "\t\"UpdateTime\": \"0\",\n" +
                    "\t\"Table\": [{\n" +
                    "\t\t\t\"Level1\": {\n" +
                    "\t\t\t\t\"ID\": \"1534\",\n" +
                    "\t\t\t\t\"Name\": \"材料\",\n" +
                    "\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\"Stocks\": [],\n" +
                    "\t\t\t\t\"IsNew\": 0\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"Level2\": [{\n" +
                    "\t\t\t\t\t\"ID\": \"1537\",\n" +
                    "\t\t\t\t\t\"Name\": \"光伏绝缘胶\",\n" +
                    "\t\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"300537\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"光伏板块的BC电池（背接触电池）用光伏绝缘胶从去年底到今年上半年已经开始逐渐放量\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"广信材料\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 999\n" +
                    "\t\t\t\t\t}]\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t{\n" +
                    "\t\t\t\t\t\"ID\": \"1539\",\n" +
                    "\t\t\t\t\t\"Name\": \"掩膜材料\",\n" +
                    "\t\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"688429\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司首创的臭氧清洗辅助品 CA，通过增强臭氧的清洗能力，彻底清洗硅片表面残留的有机物，在客户产线已完成中试\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"时创能源\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 315\n" +
                    "\t\t\t\t\t}]\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t{\n" +
                    "\t\t\t\t\t\"ID\": \"1540\",\n" +
                    "\t\t\t\t\t\"Name\": \"焊线胶带\",\n" +
                    "\t\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"603212\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司焊线胶带目前给头部BC组件厂商供货\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"赛伍技术\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 844\n" +
                    "\t\t\t\t\t}]\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t{\n" +
                    "\t\t\t\t\t\"ID\": \"1541\",\n" +
                    "\t\t\t\t\t\"Name\": \"光伏焊带\",\n" +
                    "\t\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"301266\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司开发量产了适用于BC电池的焊带产品\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"宇邦新材\",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 495\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"688516\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司最早于2014年已经和客户共同研发推出IBC串焊机。储备了丰富的BC串焊机技术\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"奥特维  \",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 327\n" +
                    "\t\t\t\t\t\t}\n" +
                    "\t\t\t\t\t]\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t]\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"Level1\": {\n" +
                    "\t\t\t\t\"ID\": \"1533\",\n" +
                    "\t\t\t\t\"Name\": \"设备\",\n" +
                    "\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\"Stocks\": [],\n" +
                    "\t\t\t\t\"IsNew\": 0\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"Level2\": [{\n" +
                    "\t\t\t\t\t\"ID\": \"1535\",\n" +
                    "\t\t\t\t\t\"Name\": \"自动化设备\",\n" +
                    "\t\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"300757\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司有用于BC电池生产的相关设备，包括槽式单晶制绒机、链式去PSG/BSG清洗机、槽式碱抛光机、去绕镀清洗机、VDI铜电镀、HDI铜电镀、槽式去膜去种子层化锡、测试分选、石英舟类自动化、石墨舟类自动化、板式自动化、包装线自动化等设备\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"罗博特科\",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 1275\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"688147\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司应用于BC类电池制造的ALD设备陆续获得来自隆基和爱旭的订单\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"微导纳米\",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 376\n" +
                    "\t\t\t\t\t\t}\n" +
                    "\t\t\t\t\t]\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t{\n" +
                    "\t\t\t\t\t\"ID\": \"1536\",\n" +
                    "\t\t\t\t\t\"Name\": \"激光设备\",\n" +
                    "\t\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"688170\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司BC电池激光开膜隔离加工技术尚处在研发布局阶段\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"德龙激光\",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 449\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"301021\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司布局了面向 BC、钙钛矿等多种电池技术所需激光器、光学模组或关键设备等业务\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"英诺激光\",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 386\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"300776\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司目前的主要产品包括 PERC 激光消融设备，PERC 电池激光掺杂设备，TOPCon 电池激光硼掺杂设备，背接触电池（BC）的激光微蚀刻设备\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"帝尔激光\",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 384\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"StockID\": \"688559\",\n" +
                    "\t\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\t\"Reason\": \"公司有产品应用于TOPCon、XBC电池及组件的激光加工设备\",\n" +
                    "\t\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\t\"prod_name\": \"海目星  \",\n" +
                    "\t\t\t\t\t\t\t\"Hot\": 324\n" +
                    "\t\t\t\t\t\t}\n" +
                    "\t\t\t\t\t]\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t]\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"Level1\": {\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"ZSCode\": \"0\",\n" +
                    "\t\t\t\t\"Stocks\": [{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"002056\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司有比较早参与研发P-IBC产品，但以目前良率还无法量产\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"横店东磁\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 1390\n" +
                    "\t\t\t\t\t},\n" +
                    "\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"002865\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司研发中心已建立基于N型技术的BC产品实验线，下步将进入中试阶段，实现N型BC产品线量产，推动N型产品升级\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"钧达股份\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 1341\n" +
                    "\t\t\t\t\t},\n" +
                    "\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"601012\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"BC产能主要为30GW HPBC（高效复合钝化背接触技术）电池项目\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"隆基绿能\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 1053\n" +
                    "\t\t\t\t\t},\n" +
                    "\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"600732\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司正式推出了基于全新一代 N 型背接触 ABC 电池技术的组件系列产品，首期珠海6.5GW ABC 电池量产项目已顺利投产\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"爱旭股份\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 878\n" +
                    "\t\t\t\t\t},\n" +
                    "\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"002795\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"子公司普乐泰兴产品规划线主要为TOPCon电池和BC电池\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"永和智控\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 649\n" +
                    "\t\t\t\t\t},\n" +
                    "\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"688223\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司已量产高效电池聚焦N型TOPCon技术，储备了IBC电池技术和钙钵矿电池技术等\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"晶科能源\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 408\n" +
                    "\t\t\t\t\t},\n" +
                    "\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\"StockID\": \"688560\",\n" +
                    "\t\t\t\t\t\t\"IsZz\": \"2\",\n" +
                    "\t\t\t\t\t\t\"IsHot\": \"0\",\n" +
                    "\t\t\t\t\t\t\"Reason\": \"公司的胶膜产品和光伏背板产品在BC电池封装上已经得到运用\",\n" +
                    "\t\t\t\t\t\t\"IsNew\": 0,\n" +
                    "\t\t\t\t\t\t\"prod_name\": \"明冠新材\",\n" +
                    "\t\t\t\t\t\t\"Hot\": 327\n" +
                    "\t\t\t\t\t}\n" +
                    "\t\t\t\t],\n" +
                    "\t\t\t\t\"IsNew\": 0\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"Level2\": []\n" +
                    "\t\t}\n" +
                    "\t],\n" +
                    "\t\"Stocks\": [],\n" +
                    "\t\"StockList\": [{\n" +
                    "\t\t\t\"StockID\": \"601012\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"BC产能主要为30GW HPBC（高效复合钝化背接触技术）电池项目\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"隆基绿能\",\n" +
                    "\t\t\t\"HotNum\": 1053\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"600732\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"公司正式推出了基于全新一代 N 型背接触 ABC 电池技术的组件系列产品，首期珠海6.5GW ABC 电池量产项目已顺利投产\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"爱旭股份\",\n" +
                    "\t\t\t\"HotNum\": 878\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"002795\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"子公司普乐泰兴产品规划线主要为TOPCon电池和BC电池\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"永和智控\",\n" +
                    "\t\t\t\"HotNum\": 649\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688560\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"公司的胶膜产品和光伏背板产品在BC电池封装上已经得到运用\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"明冠新材\",\n" +
                    "\t\t\t\"HotNum\": 327\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688223\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"公司已量产高效电池聚焦N型TOPCon技术，储备了IBC电池技术和钙钵矿电池技术等\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"晶科能源\",\n" +
                    "\t\t\t\"HotNum\": 408\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"002056\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"公司有比较早参与研发P-IBC产品，但以目前良率还无法量产\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"横店东磁\",\n" +
                    "\t\t\t\"HotNum\": 1390\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"002865\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1532\",\n" +
                    "\t\t\t\t\"Name\": \"电池组件\",\n" +
                    "\t\t\t\t\"Reason\": \"公司研发中心已建立基于N型技术的BC产品实验线，下步将进入中试阶段，实现N型BC产品线量产，推动N型产品升级\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"钧达股份\",\n" +
                    "\t\t\t\"HotNum\": 1341\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688147\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1535\",\n" +
                    "\t\t\t\t\"Name\": \"自动化设备\",\n" +
                    "\t\t\t\t\"Reason\": \"公司应用于BC类电池制造的ALD设备陆续获得来自隆基和爱旭的订单\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"微导纳米\",\n" +
                    "\t\t\t\"HotNum\": 376\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"300757\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1535\",\n" +
                    "\t\t\t\t\"Name\": \"自动化设备\",\n" +
                    "\t\t\t\t\"Reason\": \"公司有用于BC电池生产的相关设备，包括槽式单晶制绒机、链式去PSG/BSG清洗机、槽式碱抛光机、去绕镀清洗机、VDI铜电镀、HDI铜电镀、槽式去膜去种子层化锡、测试分选、石英舟类自动化、石墨舟类自动化、板式自动化、包装线自动化等设备\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"罗博特科\",\n" +
                    "\t\t\t\"HotNum\": 1275\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"301021\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1536\",\n" +
                    "\t\t\t\t\"Name\": \"激光设备\",\n" +
                    "\t\t\t\t\"Reason\": \"公司布局了面向 BC、钙钛矿等多种电池技术所需激光器、光学模组或关键设备等业务\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"英诺激光\",\n" +
                    "\t\t\t\"HotNum\": 386\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"300776\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1536\",\n" +
                    "\t\t\t\t\"Name\": \"激光设备\",\n" +
                    "\t\t\t\t\"Reason\": \"公司目前的主要产品包括 PERC 激光消融设备，PERC 电池激光掺杂设备，TOPCon 电池激光硼掺杂设备，背接触电池（BC）的激光微蚀刻设备\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"帝尔激光\",\n" +
                    "\t\t\t\"HotNum\": 384\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688170\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1536\",\n" +
                    "\t\t\t\t\"Name\": \"激光设备\",\n" +
                    "\t\t\t\t\"Reason\": \"公司BC电池激光开膜隔离加工技术尚处在研发布局阶段\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"德龙激光\",\n" +
                    "\t\t\t\"HotNum\": 449\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688559\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1536\",\n" +
                    "\t\t\t\t\"Name\": \"激光设备\",\n" +
                    "\t\t\t\t\"Reason\": \"公司有产品应用于TOPCon、XBC电池及组件的激光加工设备\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"海目星  \",\n" +
                    "\t\t\t\"HotNum\": 324\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"300537\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1537\",\n" +
                    "\t\t\t\t\"Name\": \"光伏绝缘胶\",\n" +
                    "\t\t\t\t\"Reason\": \"光伏板块的BC电池（背接触电池）用光伏绝缘胶从去年底到今年上半年已经开始逐渐放量\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"广信材料\",\n" +
                    "\t\t\t\"HotNum\": 999\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688429\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1539\",\n" +
                    "\t\t\t\t\"Name\": \"掩膜材料\",\n" +
                    "\t\t\t\t\"Reason\": \"公司首创的臭氧清洗辅助品 CA，通过增强臭氧的清洗能力，彻底清洗硅片表面残留的有机物，在客户产线已完成中试\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"时创能源\",\n" +
                    "\t\t\t\"HotNum\": 315\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"603212\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1540\",\n" +
                    "\t\t\t\t\"Name\": \"焊线胶带\",\n" +
                    "\t\t\t\t\"Reason\": \"公司焊线胶带目前给头部BC组件厂商供货\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"赛伍技术\",\n" +
                    "\t\t\t\"HotNum\": 844\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"301266\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1541\",\n" +
                    "\t\t\t\t\"Name\": \"光伏焊带\",\n" +
                    "\t\t\t\t\"Reason\": \"公司开发量产了适用于BC电池的焊带产品\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"宇邦新材\",\n" +
                    "\t\t\t\"HotNum\": 495\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"StockID\": \"688516\",\n" +
                    "\t\t\t\"Tag\": [{\n" +
                    "\t\t\t\t\"ID\": \"1541\",\n" +
                    "\t\t\t\t\"Name\": \"光伏焊带\",\n" +
                    "\t\t\t\t\"Reason\": \"公司最早于2014年已经和客户共同研发推出IBC串焊机。储备了丰富的BC串焊机技术\"\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"prod_name\": \"奥特维  \",\n" +
                    "\t\t\t\"HotNum\": 327\n" +
                    "\t\t}\n" +
                    "\t],\n" +
                    "\t\"IsNew\": 0,\n" +
                    "\t\"Power\": 1,\n" +
                    "\t\"Subscribe\": 0,\n" +
                    "\t\"ZT\": [],\n" +
                    "\t\"IsGood\": 0,\n" +
                    "\t\"GoodNum\": 48,\n" +
                    "\t\"ComNum\": 483,\n" +
                    "\t\"errcode\": \"0\",\n" +
                    "\t\"t\": 0.012028000000000039\n" +
                    "}";
        }

        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // 解析JSON为对象
            HotTopicResponse response = objectMapper.readValue(jsonData, HotTopicResponse.class);

            // 验证基本信息
            System.out.println("主题ID: " + response.getId());
            System.out.println("主题名称: " + response.getName());
            System.out.println("简介: " + response.getBriefIntro());
            System.out.println("点赞数: " + response.getGoodNum());
            System.out.println("评论数: " + response.getComNum());

            // 验证分类表信息
            if (response.getTable() != null && !response.getTable().isEmpty()) {
                System.out.println("\n分类信息:");
                for (TopicTable table : response.getTable()) {
                    CategoryLevel level1 = table.getLevel1();
                    System.out.println("一级分类: " + level1.getName() + " (ID: " + level1.getId() + ")");

                    if (table.getLevel2() != null) {
                        for (CategoryLevel level2 : table.getLevel2()) {
                            System.out.println("  二级分类: " + level2.getName() + " (ID: " + level2.getId() + ")");

                            if (level2.getStocks() != null) {
                                for (StockDetail stock : level2.getStocks()) {
                                    System.out.println("    股票: " + stock.getProdName() + " (" + stock.getStockId() + ")");
                                    System.out.println("    热度: " + stock.getHot());
                                    System.out.println("    原因: " + stock.getReason());
                                }
                            }
                        }
                    }

                    // 处理一级分类直接包含的股票
                    if (level1.getStocks() != null) {
                        for (StockDetail stock : level1.getStocks()) {
                            System.out.println("  股票: " + stock.getProdName() + " (" + stock.getStockId() + ")");
                            System.out.println("  热度: " + stock.getHot());
                        }
                    }
                }
            }

            // 验证股票列表
            if (response.getStockList() != null && !response.getStockList().isEmpty()) {
                System.out.println("\n股票列表:");
                for (StockInfo stock : response.getStockList()) {
                    System.out.println("股票: " + stock.getProdName() + " (" + stock.getStockId() + ")");
                    System.out.println("热度: " + stock.getHotNum());

                    if (stock.getTag() != null) {
                        for (StockTag tag : stock.getTag()) {
                            System.out.println("  标签: " + tag.getName() + " - " + tag.getReason());
                        }
                    }
                }
            }

            System.out.println("\n解析成功！");

        } catch (Exception e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}