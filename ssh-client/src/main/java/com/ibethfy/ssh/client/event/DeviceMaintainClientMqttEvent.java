package com.ibethfy.ssh.client.event;

import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import org.springframework.context.ApplicationEvent;

public class DeviceMaintainClientMqttEvent extends ApplicationEvent {
    public static final String TOPIC = "/client/maintain/1000";

    private MqttMessage mqttMessage;

    public DeviceMaintainClientMqttEvent(MqttMessage mqttMessage) {
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
