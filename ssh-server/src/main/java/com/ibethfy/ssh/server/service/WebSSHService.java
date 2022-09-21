package com.ibethfy.ssh.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibethfy.ssh.server.pojo.WebSSHData;
import com.ibethfy.ssh.constant.ConstantPool;
import com.ibethfy.ssh.exception.ServiceException;
import com.ibethfy.ssh.pojo.SSHCommandDto;
import com.ibethfy.ssh.pojo.CommandResultDto;
import com.ibethfy.ssh.server.pojo.SSHConnectInfo;
import com.ibethfy.ssh.utils.uuid.IdUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class WebSSHService {

    private static final String COMMAND_SEND_TOPIC = "/client/maintain/";

    private static final String REDIS_PORT_RANGE_KEY = "ssh-port-range";

    private static final String REVERSE_SSH = "reverse_ssh";

    private static final String REVERSE_SSH_PORT = "port";

    private static final String REVERSE_SSH_USER = "user";

    private static final String REVERSE_SSH_PASSWORD = "password";

    private static final String REVERSE_SSH_ADDRESS = "address";

    private static final Integer PORT_START = 10000;

    private static final Integer PORT_END = 15000;

    @Value(value = "${ssh.userName}")
    private String sshUserName;

    @Value(value = "${ssh.password}")
    private String sshPassword;

    @Value(value = "${ssh.address}")
    private String sshAddress;

    @Resource
    RedisCache redisCache;

    @Resource
    DeviceService deviceService;

    //存放ssh连接信息的map
    private static Map<String, Object> sshMap = new ConcurrentHashMap<>();

    //线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * @Description: 初始化连接
     * @Param: [session]
     * @return: void
     */
    public void initConnection(WebSocketSession session) {
        JSch jSch = new JSch();
        SSHConnectInfo sshConnectInfo = new SSHConnectInfo();
        sshConnectInfo.setJSch(jSch);
        sshConnectInfo.setWebSocketSession(session);
        String uuid = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        //将这个ssh连接信息放入map中
        sshMap.put(uuid, sshConnectInfo);
    }

    public void receiveHandle(String buffer, WebSocketSession session) {
        ObjectMapper objectMapper = new ObjectMapper();
        WebSSHData webSSHData;
        try {
            webSSHData = objectMapper.readValue(buffer, WebSSHData.class);
        } catch (IOException e) {
            log.error("Json转换异常,异常信息:{}", e.getMessage());
            return;
        }
        String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        if (ConstantPool.WEBSSH_OPERATE_CONNECT.equals(webSSHData.getOperate())) {
            if (webSSHData.getType().equals(1)) {
                handleFSSH(session, webSSHData, userId);
            } else {
                handleRSSH(session, webSSHData, userId);
            }
        } else if (ConstantPool.WEBSSH_OPERATE_COMMAND.equals(webSSHData.getOperate())) {
            String command = webSSHData.getCommand();
            SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
            if (sshConnectInfo != null) {
                try {
                    transToSSH(sshConnectInfo.getChannel(), command);
                } catch (IOException e) {
                    log.error("webssh连接异常，异常信息:{}", e.getMessage());
                    close(session);
                }
            }
        } else {
            log.error("不支持的操作");
            close(session);
        }
    }

    private void handleRSSH(WebSocketSession session, WebSSHData webSSHData, String userId) {
        //找到刚才存储的ssh连接对象
        SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
        executorService.execute(() -> {
            try {
                //启动线程异步处理
                WebSSHData finalWebSSHData = generateSSHInfo(webSSHData);
                connectToSSH(sshConnectInfo, finalWebSSHData, session);
            } catch (Exception e) {
                log.error("webssh连接异常，异常信息:{}", e.getMessage());
                close(session);
            }
        });
    }

    private WebSSHData generateSSHInfo(WebSSHData webSSHData) {
        Integer port = getFreePort();
        log.info("get free port to device: " + webSSHData.getDeviceId() + ",port is" + port);
        SSHCommandDto commandDto = getCommandDto(webSSHData, port);
        //发送mqtt消息并等待结果，可能会超时，或者抛出异常
        CommandResultDto admin = deviceService.sendCommand(commandDto);
        if (admin.getResult().equals("SUCCESS")) {
            WebSSHData webSSH = new WebSSHData();
            //命令为 ssh root@172.16.15.10 -p 10000 ，port为跳板机的端口，root为目的机器的用户名，密码为目的机器的密码
            webSSH.setHost(sshAddress);
            webSSH.setPort(port);
            webSSH.setUsername(webSSHData.getUsername());
            webSSH.setPassword(webSSHData.getPassword());
            return webSSH;
        } else {
            log.error("webssh连接异常");
            throw new ServiceException("webssh连接异常");
        }
    }

    /**
     * 本来应该用分布式锁的，但感觉没必要，冲突的可能较小，只在单节点加锁
     *
     * @return
     */
    private synchronized Integer getFreePort() {
        //从redis里面选一个端口出来，用于创建连接，端口范围为10000-15000
        Integer port = redisCache.getCacheObject(REDIS_PORT_RANGE_KEY);
        //没有就从10000开始
        if (Objects.isNull(port) || port >= PORT_END) {
            redisCache.setCacheObject(REDIS_PORT_RANGE_KEY, PORT_START);
            port = PORT_START;
        } else {
            //检验端口是否被占用，并获取一个有效的端口
            port++;
            redisCache.setCacheObject(REDIS_PORT_RANGE_KEY, port);
        }
        return port;
    }

    private SSHCommandDto getCommandDto(WebSSHData webSSHData, Integer port) {
        //发送MQTT命令给客户端，客户端执行相应操作
        SSHCommandDto commandDto = new SSHCommandDto();
        commandDto.setDeviceId(webSSHData.getDeviceId());
        commandDto.setServerAddress(sshAddress);
        commandDto.setServerPort(port);
        commandDto.setServerPassword(sshPassword);
        commandDto.setServerUserName(sshUserName);
        commandDto.setUuid(IdUtils.fastSimpleUUID());
        return commandDto;
    }

    private void handleFSSH(WebSocketSession session, WebSSHData webSSHData, String userId) {
        //找到刚才存储的ssh连接对象
        SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
        //启动线程异步处理
        WebSSHData finalWebSSHData = webSSHData;
        executorService.execute(() -> {
            try {
                connectToSSH(sshConnectInfo, finalWebSSHData, session);
            } catch (JSchException | IOException e) {
                log.error("webssh连接异常，异常信息:{}", e.getMessage());
                close(session);
            }
        });
    }

    public void sendMessage(WebSocketSession session, byte[] buffer) throws IOException {
        session.sendMessage(new TextMessage(buffer));
    }

    public void close(WebSocketSession session) {
        try {
            String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
            SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
            if (sshConnectInfo != null) {
                //断开连接
                if (sshConnectInfo.getChannel() != null) sshConnectInfo.getChannel().disconnect();
                //map中移除
                sshMap.remove(userId);
            }
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description: 使用jsch连接终端
     * @Param: [cloudSSH, webSSHData, webSocketSession]
     * @return: void
     */
    private void connectToSSH(SSHConnectInfo sshConnectInfo, WebSSHData webSSHData, WebSocketSession webSocketSession) throws JSchException, IOException {
        Session session;
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        //获取jsch的会话
        session = sshConnectInfo.getJSch().getSession(webSSHData.getUsername(), webSSHData.getHost(), webSSHData.getPort());
        session.setConfig(config);
        //设置密码
        session.setPassword(webSSHData.getPassword());
        //连接  超时时间30s
        session.connect(30000);

        //开启shell通道
        Channel channel = session.openChannel("shell");

        //通道连接 超时时间3s
        channel.connect(3000);

        //设置channel
        sshConnectInfo.setChannel(channel);

        //转发消息
        transToSSH(channel, "\r");

        //读取终端返回的信息流
        InputStream inputStream = channel.getInputStream();
        try {
            //循环读取
            byte[] buffer = new byte[1024];
            int i = 0;
            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
            while ((i = inputStream.read(buffer)) != -1) {
                sendMessage(webSocketSession, Arrays.copyOfRange(buffer, 0, i));
            }

        } finally {
            //断开连接后关闭会话
            session.disconnect();
            channel.disconnect();
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

    /**
     * @Description: 将消息转发到终端
     * @Param: [channel, data]
     * @return: void
     */
    private void transToSSH(Channel channel, String command) throws IOException {
        if (channel != null) {
            OutputStream outputStream = channel.getOutputStream();
            outputStream.write(command.getBytes());
            outputStream.flush();
        }
    }
}
