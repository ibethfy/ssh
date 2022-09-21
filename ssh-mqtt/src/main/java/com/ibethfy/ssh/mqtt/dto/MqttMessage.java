package com.ibethfy.ssh.mqtt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqttMessage {
    private String topic;

    private Object data;
}
