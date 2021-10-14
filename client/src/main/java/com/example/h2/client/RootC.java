package com.example.h2.client;

import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

@RestController
public class RootC {
    @Autowired
    @Qualifier(("HTTP21"))
    WebClient webClient;
    static HttpClient client;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    TaskExecutor taskExecutor;

    @GetMapping("/home")
    public String home() {
        return "hello";
    }

    static {
        client = HttpClient.create().protocol(HttpProtocol.H2C, HttpProtocol.HTTP11);

        client.headers(headers -> {
            headers.add("Connection", "Upgrade, HTTP2-Settings");
            headers.add("Upgrade", "h2c");
            headers.add("HTTP2-Settings", "");
        });
    }

    @GetMapping("/call2")
    public Mono<String> call2() {
        // 服务器支持H2C,CLIENT支持H2C和H1，通过wireshark抓包可以看到协议升级过程
        return webClient.get().uri("http://10.19.215.76/home").retrieve().bodyToMono(String.class);
    }

    @GetMapping("/call1")
    public Mono<String> call1() {
        // 服务器不支持H2C，通过wireshark抓包可以看到协议是h1。这说明用同一个客户端可以支持2中协议。
        return webClient.get().uri("http://10.19.215.76:89/home").retrieve().bodyToMono(String.class);
    }

    // 串行调用接口，之所以不用webClient是因为bug，不过webclient底层就是HttpClient，效果一样的
    @GetMapping("/test2NettySerial")
    public long test2() {
        // 第一次请求涉及到协议切换耗时较长
        client.get().uri("http://10.19.215.76/home").responseContent().aggregate().asString().block();
        // 服务器支持H2C,CLIENT支持H2C和H1
        long time = System.currentTimeMillis();

        for (int i = 0; i < 500; i++) {
            client.get().uri("http://10.19.215.76/home").responseContent().aggregate().asString().block();
        }
        return System.currentTimeMillis() - time;
    }

    @GetMapping("/test1NettySerial")
    public long test1_netty() {
        client.get().uri("http://10.19.215.76:89/home").responseContent().aggregate().asString().block();
        long time = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            client.get().uri("http://10.19.215.76:89/home").responseContent().aggregate().asString().block();
        }
        return System.currentTimeMillis() - time;
    }

    @GetMapping("/test1RestTemplateSerial")
    public long test1RestTemplate() {
        String s = restTemplate.getForObject("http://10.19.215.76:89/home", String.class);
        // 服务器支持H1
        long time = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            restTemplate.getForObject("http://10.19.215.76:89/home", String.class);
        }
        return System.currentTimeMillis() - time;
    }

    // 测试netty客户端并发调用h2接口
    @GetMapping("/test2NettyConcurrent")
    public long test2NettyConcurrent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(500);
        // 第一次请求涉及到协议切换耗时较长
        client.get().uri("http://10.19.215.76/home").responseContent().aggregate().asString().block();
        // 服务器支持H2C,CLIENT支持H2C和H1
        long time = System.currentTimeMillis();

        for (int i = 0; i < 500; i++) {
            taskExecutor.execute(() -> {
                client.get().uri("http://10.19.215.76/home").responseContent().aggregate().asString().block();
                latch.countDown();
            });

        }
        latch.await();
        return System.currentTimeMillis() - time;
    }

    // 测试Resttemplate客户端并发调用h1接口，resttemplate默认连接池是啥效果还没看
    @GetMapping("/test1RestTemplateConcurrent")
    public long test1RestTemplateConcurrent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(500);
        // 为了对比公平，跟h2c一样先发一个请求建立连接
        restTemplate.getForObject("http://10.19.215.76:89/home", String.class);

        long time = System.currentTimeMillis();

        for (int i = 0; i < 500; i++) {
            taskExecutor.execute(() -> {
                restTemplate.getForObject("http://10.19.215.76:89/home", String.class);
                latch.countDown();
            });

        }
        latch.await();
        return System.currentTimeMillis() - time;
    }

}
