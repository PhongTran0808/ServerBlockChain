package com.cuutrominhbach.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.TimeUnit;

@Configuration
public class Web3jConfig {

    @Value("${web3j.rpc-url}")
    private String rpcUrl;

    @Value("${web3j.admin-private-key}")
    private String adminPrivateKey;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    @Bean
    public Web3j web3j() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        HttpService httpService = new HttpService(rpcUrl, httpClient);
        return Web3j.build(httpService);
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(adminPrivateKey);
    }

    @Bean
    public String contractAddress() {
        return contractAddress;
    }
}
