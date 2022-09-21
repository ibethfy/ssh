package com.ibethfy.ssh.server.service;

import com.alibaba.fastjson2.JSONObject;
import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import com.ibethfy.ssh.mqtt.handler.MqttMessageSender;
import com.ibethfy.ssh.server.event.DeviceMaintainMqttEvent;
import com.ibethfy.ssh.exception.ServiceException;
import com.ibethfy.ssh.pojo.SSHCommandDto;
import com.ibethfy.ssh.pojo.CommandResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DeviceService{

    private static final String COMMAND_SEND_TOPIC = "/client/maintain/";

    private static final Integer MAX_TIME = 30;

    @Resource
    RedisCache redisCache;

    @Resource
    MqttMessageSender mqttMessageSender;

    public CommandResultDto sendCommand(SSHCommandDto commandDto) {
        String deviceId = commandDto.getDeviceId();
        String topic = COMMAND_SEND_TOPIC + deviceId;
        mqttMessageSender.send(topic, JSONObject.toJSONString(commandDto));
        return handleCommandExecResult(commandDto);
    }

    private CommandResultDto handleCommandExecResult(SSHCommandDto commandDto) {
        try {
            //循环等待结果，超时
            int count = 0;
            boolean flag = true;
            while (flag) {
                CommandResultDto result = redisCache.getCacheObject(commandDto.getUuid());
                if (Objects.isNull(result)) {
                    Thread.sleep(1000);
                    count++;
                    if (count > MAX_TIME) {
                        flag = false;
                    }
                    continue;
                }
                return result;
            }
        } catch (Exception e) {
            log.error("handle command result error", e);
            throw new ServiceException("获取执行结果异常，请检查设备状态");
        }
        throw new ServiceException("获取执行结果异常，请检查设备状态");
    }

    @EventListener
    @Transactional
    public void handleMqttDeviceMaintainEvent(DeviceMaintainMqttEvent deviceMaintainMqttEvent) {
        MqttMessage mqttMessage = deviceMaintainMqttEvent.getMqttMessage();
        Object data = mqttMessage.getData();
        try {
            JSONObject jsonObject = JSONObject.parseObject(data.toString());
            CommandResultDto commandReceiveDto = jsonObject.to(CommandResultDto.class);
            //放到redis里面等待处理
            redisCache.setCacheObject(commandReceiveDto.getUuid(), commandReceiveDto, 5 * 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("handle device maintain mqtt message error, data is: " + data, e);
        }
    }
}
