package com.ibethfy.ssh.client.mqtt;

import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import com.ibethfy.ssh.mqtt.event.MqttReceiveEvent;
import com.ibethfy.ssh.client.event.DeviceMaintainClientMqttEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CommonMqttMessageHandler {
    @Resource
    ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void handleMqttMessage(MqttReceiveEvent mqttReceiveEvent)
    {
        MqttMessage mqttMessage = mqttReceiveEvent.getMqttMessage();
        switch (mqttMessage.getTopic())
        {
            case DeviceMaintainClientMqttEvent.TOPIC:
                applicationEventPublisher.publishEvent(new DeviceMaintainClientMqttEvent(mqttMessage));
                break;
            default:
                break;
        }
    }
}
