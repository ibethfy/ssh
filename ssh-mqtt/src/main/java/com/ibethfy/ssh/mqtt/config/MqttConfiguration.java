package com.ibethfy.ssh.mqtt.config;

import lombok.Data;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Data
@Component
@Configuration
@ConfigurationProperties("spring.mqtt")
public class MqttConfiguration {

    /**
     * 链接用户名
     */
    private String username;


    /**
     * 链接密码
     */
    private String password;


    /**
     * 链接地址
     */
    private String url;


    /**
     * 客户端ID 前缀
     */
    private String clientId;


    private Integer completionTimeout = 2000;

    /**
     * 默认订阅topic
     */
    private String inputTopic;

    /**
     * 默认订阅topic
     */
    private String defaultInputTopic;


    /**
     * 默认发布topic
     */
    private String defaultOutTopic;

    /**
     * 默认QOS
     */
    private Integer defaultQos;


    /**
     * 注册MQTT客户端工厂
     *
     * @return MqttPahoClientFactory
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() throws Exception {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(0);
        options.setKeepAliveInterval(90);
        options.setAutomaticReconnect(true);
        options.setUserName(this.getUsername());
        options.setPassword(this.getPassword().toCharArray());
        options.setServerURIs(new String[]{this.getUrl()});
        //需要双向认证时开启。
//        options.setSocketFactory(SslUtil.getSocketFactory(sslPath + "ca.pem", sslPath + "client.pem", sslPath + "client.key", password));
        factory.setConnectionOptions(options);
        return factory;
    }
}