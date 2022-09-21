package com.ibethfy.ssh.mqtt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.stream.CharacterStreamReadingMessageSource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.annotation.Resource;
import java.util.UUID;

@Configuration
public class MqttOutboundConfiguration {


    @Resource
    private MqttConfiguration mqttConfiguration;

    @Resource
    private MqttPahoClientFactory factory;




    @Bean
    public IntegrationFlow mqttOutFlow() {
        return IntegrationFlows.from(CharacterStreamReadingMessageSource.stdin(),
                        e -> e.poller(Pollers.fixedDelay(2000)))
                .transform(p -> p + "")
                .handle(mqttOutbound())
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(UUID.randomUUID().toString(), factory);
        messageHandler.setDefaultQos(mqttConfiguration.getDefaultQos());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(mqttConfiguration.getDefaultOutTopic());
        return messageHandler;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }






}