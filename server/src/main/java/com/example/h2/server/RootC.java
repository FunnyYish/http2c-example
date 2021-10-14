package com.example.h2.server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootC {

    @GetMapping("/home")
    public String home() throws InterruptedException {
        // 模拟业务处理耗时
        Thread.sleep(500);
        return "hello";
    }

}
