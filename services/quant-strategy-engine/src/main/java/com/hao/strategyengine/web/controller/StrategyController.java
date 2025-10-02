package com.hao.strategyengine.web.controller;

import com.hao.strategyengine.model.market.MarketData;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.engine.StrategyEngine;
import com.hao.strategyengine.event.listener.MetricsEventListener;
import com.hao.strategyengine.factory.StrategyFactory;
import com.hao.strategyengine.model.request.MarketDataRequest;
import com.hao.strategyengine.model.request.StrategyCreateRequest;
import com.hao.strategyengine.model.response.StrategyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 策略控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/strategy")
public class StrategyController {
    
    private final StrategyEngine strategyEngine;
    private final StrategyFactory strategyFactory;
    private final MetricsEventListener metricsListener;
    
    public StrategyController(StrategyEngine strategyEngine,
                             StrategyFactory strategyFactory,
                             MetricsEventListener metricsListener) {
        this.strategyEngine = strategyEngine;
        this.strategyFactory = strategyFactory;
        this.metricsListener = metricsListener;
    }
    
    /**
     * 创建策略
     */
    @PostMapping("/create")
    public ResponseEntity<StrategyResponse> createStrategy(
            @RequestBody StrategyCreateRequest request) {
        
        try {
            Strategy strategy = strategyFactory.createStrategy(request.getConfig());
            
            // TODO: 注册到引擎
            
            return ResponseEntity.ok(StrategyResponse.success("策略创建成功"));
        } catch (Exception e) {
            log.error("创建策略失败", e);
            return ResponseEntity.badRequest()
                .body(StrategyResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取策略状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStrategyStatus() {
        Map<String, String> status = strategyEngine.getStrategyStatus();
        return ResponseEntity.ok(status);
    }
    /**
     * 模拟市场数据推送
     */
    @PostMapping("/market-data")
    public ResponseEntity<String> pushMarketData(@RequestBody MarketDataRequest request) {
        try {
            MarketData marketData = new MarketData();
            marketData.setSymbol(request.getSymbol());
            marketData.setPrice(request.getPrice());
            marketData.setVolume(request.getVolume());
            marketData.setTimestamp(LocalDateTime.now());

            strategyEngine.onMarketData(marketData);

            return ResponseEntity.ok("市场数据处理成功");
        } catch (Exception e) {
            log.error("处理市场数据失败", e);
            return ResponseEntity.badRequest().body("处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取策略指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<MetricsEventListener.MetricsData> getMetrics() {
        MetricsEventListener.MetricsData metrics = metricsListener.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * 暂停所有策略
     */
    @PostMapping("/pause")
    public ResponseEntity<String> pauseAll() {
        strategyEngine.pauseAll();
        return ResponseEntity.ok("所有策略已暂停");
    }

    /**
     * 恢复所有策略
     */
    @PostMapping("/resume")
    public ResponseEntity<String> resumeAll() {
        strategyEngine.resumeAll();
        return ResponseEntity.ok("所有策略已恢复");
    }

    /**
     * 删除策略
     */
    @DeleteMapping("/{symbol}")
    public ResponseEntity<String> deleteStrategy(@PathVariable String symbol) {
        try {
            strategyEngine.unregisterStrategy(symbol);
            return ResponseEntity.ok("策略删除成功");
        } catch (Exception e) {
            log.error("删除策略失败", e);
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }
}

