package com.example.h2.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@RestController
public class RootC {
    @Autowired
    @Qualifier(("HTTP21"))
    WebClient webClient;

    @GetMapping("/home")
    public String home() {
        return "hello";
    }

    @GetMapping("/call2")
    public Mono<String> call2() {
        // 服务器支持H2C,CLIENT支持H2C和H1
        return webClient.get().uri("http://10.19.215.76/home").retrieve().bodyToMono(String.class);
    }

    @GetMapping("/call1")
    public Mono<String> call1() {
        // 服务器不支持H2C，client支持H1并且upgrade
        return webClient.get().uri("http://10.19.215.76:89/home").retrieve().bodyToMono(String.class);
    }

}
