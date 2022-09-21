package com.ibethfy.ssh.mqtt.event;

import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import org.springframework.context.ApplicationEvent;

public class MqttReceiveEvent extends ApplicationEvent {

    private MqttMessage mqttMessage;

    public MqttReceiveEvent(MqttMessage mqttMessage) {
        super(mqttMessage);
        this.mqttMessage = mqttMessage;
    }

    public MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    public void setMqttMessage(MqttMessage mqttMessage) {
        this.mqttMessage = mqttMessage;
    }
}
