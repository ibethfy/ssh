package com.ibethfy.ssh.mqtt.handler;

import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import com.ibethfy.ssh.mqtt.event.MqttReceiveEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @date 2022/2/14 14:42
 */
@Slf4j
@Component
public class MqttMessageReceiver implements MessageHandler {

    @Resource
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            MessageHeaders headers = message.getHeaders();
            //获取消息Topic
            String receivedTopic = (String) headers.get(MqttHeaders.RECEIVED_TOPIC);
            log.debug("[获取到的消息的topic]:{} ", receivedTopic);
            //获取消息体
            String payload = (String) message.getPayload();
            log.debug("[获取到的消息的payload]:{} ", payload);
            //发布事件
            applicationEventPublisher.publishEvent(new MqttReceiveEvent(new MqttMessage(receivedTopic, payload)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}