package com.hao.datacollector.web.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * @description: 本地缓存配置，每一个bean对应一种缓存策略
 */
@Configuration
public class CacheConfig {

    public static final int NORMAL_EXPIRE_TIME = 8;

    public static final int OCEAN_DATA_EXPIRE_TIME = 1;

    public static final int HOT_STOCK_INIT_NUM = 1000;

    public static final int HOT_STOCK_MAX_NUM = 2000;

    /**
     * 缓存管理器1
     *
     * @return
     */
    @Bean("dateCaffeineCacheManager")
    public CacheManager dateCaffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                // 写入缓存后8小时失效
                .expireAfterWrite(8, TimeUnit.HOURS)
                // 该大小指的是每个cacheName下面对应容器的初始大小
                .initialCapacity(1)
                // 该大小指的是每个cacheName下面对应容器的最大容量
                .maximumSize(10));
        return caffeineCacheManager;
    }

//    /**
//     * 缓存管理器2
//     *
//     * @return
//     */
//    @Bean("caffeineCacheManager2")
//    public CacheManager cacheManager2() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存后1小时失效
//                .expireAfterWrite(20, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(200));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 缓存管理器3
//     *
//     * @return
//     */
//    @Bean("caffeineCacheManager3")
//    public CacheManager cacheManager3() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存后4小时失效
//                .expireAfterWrite(4, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(200));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 缓存管理器4
//     *
//     * @return
//     */
//    @Bean("caffeineCacheManager4")
//    public CacheManager cacheManager4() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存后60秒失效
//                .expireAfterWrite(60, TimeUnit.SECONDS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(1000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(20000));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * ocean缓存管理器，管理存储个股ocean数据的cache，失效时间1小时
//     *
//     * @return ocean缓存管理器
//     */
//    @Bean("oceanCacheManager")
//    public CacheManager oceanCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存1小时后失效
//                .expireAfterWrite(OCEAN_DATA_EXPIRE_TIME, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(HOT_STOCK_INIT_NUM)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(HOT_STOCK_MAX_NUM));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 大表数据缓存管理器，管理独立于个股的大表数据的cache
//     *
//     * @return 大表数据缓存管理器
//     */
//    @Bean("tableDataCacheManager")
//    public CacheManager tableDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存8小时后失效
//                .expireAfterWrite(NORMAL_EXPIRE_TIME, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(1)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(1));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 个股数据缓存管理器，管理只与个股（windCode）相关的数据的cache
//     *
//     * @return 个股数据缓存管理器
//     */
//    @Bean("stockDataCacheManager")
//    public CacheManager stockDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存8小时后失效
//                .expireAfterWrite(NORMAL_EXPIRE_TIME, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(HOT_STOCK_INIT_NUM)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(HOT_STOCK_MAX_NUM));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 条件数据缓存管理器，管理以复杂条件作为维度的数据的cache
//     * 数量级：1百 - 1千
//     *
//     * @return 个股条件数据缓存管理器
//     */
//    @Bean("conditionHundredDataCacheManager")
//    public CacheManager conditionHundredDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存8小时后失效
//                .expireAfterWrite(NORMAL_EXPIRE_TIME, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(100)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(1000));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 条件数据缓存管理器，管理以复杂条件作为维度的数据的cache
//     * 数量级：1千 - 1万
//     *
//     * @return 个股条件数据缓存管理器
//     */
//    @Bean("conditionThousandDataCacheManager")
//    public CacheManager conditionThousandDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存8小时后失效
//                .expireAfterWrite(NORMAL_EXPIRE_TIME, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(1000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(10000));
//        return caffeineCacheManager;
//    }
//
//    /**
//     * 条件数据缓存管理器，管理以复杂条件作为维度的数据的cache
//     * 数量级：1万 - 10万
//     *
//     * @return 个股条件数据缓存管理器
//     */
//    @Bean("conditionTenThousandDataCacheManager")
//    public CacheManager conditionTenThousandDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存8小时后失效
//                .expireAfterWrite(NORMAL_EXPIRE_TIME, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//
//    @Bean("recentTradeCacheManager")
//    public CacheManager recentTradeCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("recentChartCacheManager")
//    public CacheManager recentChartCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("recentFundCacheManager")
//    public CacheManager recentFundCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("recentHoldingCacheManager")
//    public CacheManager recentHoldingCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("crmCacheManager")
//    public CacheManager crmCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("searchKeyBoardManager")
//    public CacheManager searchKeyBoardManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存1小时后失效
//                .expireAfterWrite(1, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("matchCacheManager")
//    public CacheManager matchCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("historyFundCacheManager")
//    public CacheManager historyFundCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存5分钟后失效
//                .expireAfterWrite(5, TimeUnit.MINUTES)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("sectorDirectionCacheManager")
//    public CacheManager sectorDirectionCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存1小时后失效
//                .expireAfterWrite(1, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("relateWindCodeCacheManager")
//    public CacheManager relateWindCodeCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存1小时后失效
//                .expireAfterWrite(1, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("cloudCommonCacheManager")
//    public CacheManager cloudCommonCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存4小时后失效
//                .expireAfterWrite(4, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("wstockQuotationCacheManager")
//    public CacheManager wstockQuotationCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存6s后失效
//                .expireAfterWrite(6, TimeUnit.SECONDS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("summaryByWindCodeCacheManager")
//    public CacheManager summaryByWindCodeCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存1小时后失效
//                .expireAfterWrite(1, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(10000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(100000));
//        return caffeineCacheManager;
//    }
//
//    @Bean("edbDataCacheManager")
//    public CacheManager edbDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存6小时后失效
//                .expireAfterWrite(1, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(1000)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(10000));
//        return caffeineCacheManager;
//    }
//
//
//    @Bean("smartBodyDataCacheManager")
//    public CacheManager smartBodyDataCacheManager() {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                // 写入缓存6小时后失效
//                .expireAfterWrite(1, TimeUnit.HOURS)
//                // 该大小指的是每个cacheName下面对应容器的初始大小
//                .initialCapacity(100)
//                // 该大小指的是每个cacheName下面对应容器的最大容量
//                .maximumSize(1000));
//        return caffeineCacheManager;
//    }

}