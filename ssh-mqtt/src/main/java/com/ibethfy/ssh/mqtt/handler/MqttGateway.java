package com.ibethfy.ssh.mqtt.handler;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 消息发送网关
 * @date 2022/2/14 16:56
 */
@MessagingGateway
@Component
public interface MqttGateway {

    @Gateway(requestChannel = "mqttOutboundChannel")
    void send(@Header(MqttHeaders.TOPIC) String a, Message<byte[]> out);

    @Gateway(requestChannel = "mqttOutboundChannel")
    void send(@Header(MqttHeaders.TOPIC) String a, String message);

    @Gateway(requestChannel = "mqttOutboundChannel")
    void send(@Header(MqttHeaders.TOPIC) String a, @Header(MqttHeaders.QOS) int qos, String message);

    @Gateway(requestChannel = "mqttOutboundChannel")
    void send(@Header(MqttHeaders.TOPIC) String a, @Header(MqttHeaders.QOS) int qos, byte[] message);

}