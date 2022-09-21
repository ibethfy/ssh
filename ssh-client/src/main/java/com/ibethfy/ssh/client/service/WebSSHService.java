package com.ibethfy.ssh.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.ibethfy.ssh.mqtt.dto.MqttMessage;
import com.ibethfy.ssh.client.event.DeviceMaintainClientMqttEvent;
import com.ibethfy.ssh.mqtt.handler.MqttMessageSender;
import com.ibethfy.ssh.pojo.CommandResultDto;
import com.ibethfy.ssh.pojo.SSHCommandDto;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

@Service
@Slf4j
public class WebSSHService {
    private static final String TOPIC = "/server/maintain";

    @Value("${ssh.client.userName}")
    private String userName;

    @Value("${ssh.client.password}")
    private String password;

    @Value("${ssh.client.address}")
    private String address;

    @Value("${ssh.client.port}")
    private Integer port;

    private Session session;

    @Resource
    MqttMessageSender mqttMessageSender;

    @PostConstruct
    public void initJsch() {
        try {
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jSch = new JSch();
            //获取jsch的会话
            session = jSch.getSession(userName, address, port);
            session.setConfig(config);
            //设置密码
            session.setPassword(password);
            //连接  超时时间30s
            session.connect(30000);
        } catch (Exception e) {
            log.error("create seesion failed", e);
            //断开连接后关闭会话
            if (session != null) {
                session.disconnect();
            }
        }
    }

    @EventListener
    @Transactional
    public void handleMqttDeviceMaintainEvent(DeviceMaintainClientMqttEvent deviceMaintainCilentMqttEvent) {
        MqttMessage mqttMessage = deviceMaintainCilentMqttEvent.getMqttMessage();
        Object data = mqttMessage.getData();
        try {
            JSONObject jsonObject = JSONObject.parseObject(data.toString());
            SSHCommandDto sshCommandDto = jsonObject.to(SSHCommandDto.class);
            //sshpass -p "root123" ssh -o StrictHostKeyChecking=no -f -N -R 10037:localhost:22 root@172.16.15.10
            String command = "sshpass -p \"" + sshCommandDto.getServerPassword() + "\" ssh -o StrictHostKeyChecking=no -f -N -R "
                    + sshCommandDto.getServerPort() + ":localhost:22 " + sshCommandDto.getServerUserName() + "@" + sshCommandDto.getServerAddress();
            log.info("command is: " + command);
            execSshCommand(command);
            sendMqttMessage(sshCommandDto);
        } catch (Exception e) {
            log.error("handle device maintain mqtt message error, data is: " + data, e);
        }
    }

    private void sendMqttMessage(SSHCommandDto sshCommandDto) {
        CommandResultDto commandResultDto = new CommandResultDto();
        commandResultDto.setDeviceId(sshCommandDto.getDeviceId());
        commandResultDto.setResult("SUCCESS");
        commandResultDto.setUuid(sshCommandDto.getUuid());
        mqttMessageSender.send(TOPIC, JSONObject.toJSONString(commandResultDto));
    }

    private void execSshCommand(String command) {
        try {
            //开启shell通道
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            //转发消息
            channel.setCommand(command);
            //通道连接 超时时间3s
            channel.connect(3000);
        } catch (Exception e) {
            log.error("send command error", e);
        }
    }

    private void transToSSH(Channel channel, String command) throws IOException {
        if (channel != null) {
            OutputStream outputStream = channel.getOutputStream();
            outputStream.write(command.getBytes());
            outputStream.flush();
        }
    }
}
