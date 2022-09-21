package com.ibethfy.ssh.server.event;

import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import org.springframework.context.ApplicationEvent;

public class DeviceMaintainMqttEvent extends ApplicationEvent {
    public static final String TOPIC = "/server/maintain";

    private MqttMessage mqttMessage;

    public DeviceMaintainMqttEvent(MqttMessage mqttMessage) {
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
