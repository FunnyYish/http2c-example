package com.example.h2.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

@SpringBootApplication
public class ClientApplication {
	@Bean("HTTP21")
	public WebClient webClient2() {
		HttpClient client = HttpClient.create().protocol(HttpProtocol.H2C, HttpProtocol.HTTP11);
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).defaultHeaders(headers -> {
			headers.add("Connection", "Upgrade, HTTP2-Settings");
			// headers.setConnection(HttpHeaders.UPGRADE);
			headers.setUpgrade("h2c");
			headers.add("HTTP2-Settings", "");
		}).build();

	}

	@Bean("RestTemplate")
	public RestTemplate restTemplate() {
		return new RestTemplate();

	}

	@Bean("TaskExecutor")
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(500);
		taskExecutor.initialize();
		return taskExecutor;
	}

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

}
