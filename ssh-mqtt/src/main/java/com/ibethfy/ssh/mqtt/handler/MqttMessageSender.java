package com.ibethfy.ssh.mqtt.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class MqttMessageSender {

    private MqttGateway mqttGateway;

    public void send(String topic, String message) {
        mqttGateway.send(topic, message);
    }

    /**
     * @param topic
     * @param messageBody
     */
    public void send(String topic, int qos, String messageBody) {
        mqttGateway.send(topic, qos, messageBody);
    }

    public void send(String topic, int qos, byte[] message) {
        mqttGateway.send(topic, qos, message);
    }
}