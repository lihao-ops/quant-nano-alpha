package com.hao.strategyengine.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DragonTwoServiceImplTest {

    @Autowired
    private DragonTwoServiceImpl dragonTwoService;

    @Test
    void getDragonTwoInfo() {
        dragonTwoService.getDragonTwoInfo();
    }
}