package com.regionalai.floatingball.server.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(@Value("${floating-ball.ai.connect-timeout-ms}") int connectTimeoutMs,
                               @Value("${floating-ball.ai.read-timeout-ms}") int readTimeoutMs,
                               @Value("${floating-ball.ai.proxy.enabled:false}") boolean proxyEnabled,
                               @Value("${floating-ball.ai.proxy.host:}") String proxyHost,
                               @Value("${floating-ball.ai.proxy.port:7890}") int proxyPort,
                               @Value("${floating-ball.ai.proxy.username:}") String proxyUsername,
                               @Value("${floating-ball.ai.proxy.password:}") String proxyPassword) {
        HttpClient httpClient = HttpClient.create()
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs));

        if (proxyEnabled && proxyHost != null && !proxyHost.trim().isEmpty()) {
            httpClient = httpClient.proxy(proxy -> {
                ProxyProvider.Builder builder = proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost.trim())
                    .port(proxyPort);
                if (proxyUsername != null && !proxyUsername.trim().isEmpty()) {
                    builder.username(proxyUsername.trim());
                    builder.password(ignored -> proxyPassword == null ? "" : proxyPassword);
                }
            });
        }

        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecConfigurer -> codecConfigurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .build();
    }
}
