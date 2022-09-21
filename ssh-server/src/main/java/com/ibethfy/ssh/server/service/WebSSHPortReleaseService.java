package com.ibethfy.ssh.server.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebSSHPortReleaseService {

    private static final String REDIS_PORT_RANGE_KEY = "ssh-port-range";

    private static final Integer PORT_START = 10000;

    @Value(value = "${ssh.userName}")
    private String sshUserName;

    @Value(value = "${ssh.password}")
    private String sshPassword;

    @Value(value = "${ssh.address}")
    private String sshAddress;

    private Session session;

    @Resource
    RedisCache redisCache;

    @PostConstruct
    public void initJsch() {
        try {
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jSch = new JSch();
            //获取jsch的会话
            session = jSch.getSession(sshUserName, sshAddress);
            session.setConfig(config);
            //设置密码
            session.setPassword(sshPassword);
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

    @Scheduled(cron = "0 0 0/12 * * ?")
    public void releasePort() {
        Set<Integer> allPort = getAllPort();
        allPort.forEach(port -> {
            try {
                String command = "netstat -anp |grep " + port;
                // 查询端口命令 linux 与 windows区分
                log.info("执行命令:" + command);
                // 读取内容
                List<String> read = getPortList(command, port);
                if (read.size() == 0) {
                    log.info("未查询到端口被占用");
                    return;
                } else {
                    // 关闭端口
                    kill(read);
                }
            } catch (Exception e) {
                log.error("release port failed", e);
            }
        });
    }

    private Set<Integer> getAllPort() {
        HashSet<Integer> ports = new HashSet<>();
        Integer portNow = redisCache.getCacheObject(REDIS_PORT_RANGE_KEY);
        for (int i = PORT_START; i < portNow; i++) {
            ports.add(i);
        }
        return ports;
    }

    /**
     * @Description: 使用jsch连接终端
     * @Param: [cloudSSH, webSSHData, webSocketSession]
     * @return: void
     */
    public List<String> getPortList(String command, Integer port) throws Exception {
        //开启shell通道
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        //转发消息
        channel.setCommand(command);
        //通道连接 超时时间3s
        channel.connect(3000);
        //读取终端返回的信息流
        List<String> read = new ArrayList<>();
        InputStream inputStream = channel.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                // 验证端口
                boolean validPort = validPort(line, port);
                if (validPort) {
                    // 添加内容
                    read.add(line);
                }
            }
            inputStream.close();
            reader.close();
        } catch (Exception e) {
            log.error("send command error", e);
        } finally {
            //断开连接后关闭会话
            channel.disconnect();
            IOUtils.closeQuietly(inputStream);
        }
        return read;
    }

    /**
     * 验证此行是否为指定的端口，因为 findstr命令会是把包含的找出来，例如查找80端口，但是会把8099查找出来
     *
     * @param str
     * @return
     */
    private static boolean validPort(String str, Integer port) {
        String find = "";
        // linux TCP    0.0.0.0:12349          0.0.0.0:0              LISTENING       30700
        // windows tcp        0      0 0.0.0.0:8888            0.0.0.0:*               LISTEN      2319/python
        String reg = ":[0-9]+";
        // 匹配正则
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(str);
        Boolean findFlag = matcher.find();
        log.info("读取数据：" + str);
        // 未匹配到则直接返回
        if (!findFlag) {
            return false;
        }
        // 获取匹配内容
        find = matcher.group();
        // 处理数据
        int spstart = find.lastIndexOf(":");
        // 截取掉冒号
        find = find.substring(spstart + 1);
        int portResult = 0;
        try {
            portResult = Integer.parseInt(find);
        } catch (NumberFormatException e) {
            return false;
        }
        // 端口在其中 则通过验证
        return port.equals(portResult);
    }

    /**
     * 更换为一个Set，去掉重复的pid值
     *
     * @param data
     */
    private void kill(List<String> data) throws Exception {
        //先检测是不是都是sshd，然后是不是监听端口
        List<String> sshdListenerList = data.stream().filter(line -> line.contains("sshd") && line.contains("LISTEN")).collect(Collectors.toList());
        if (sshdListenerList.isEmpty()) {
            return;
        }

        //再检测有没有ESTABLISHED的监听数据，有的话就不能关闭
        List<String> sshdEstablishList = data.stream().filter(line -> line.contains("sshd") && line.contains("ESTABLISHED")).collect(Collectors.toList());
        if (!sshdEstablishList.isEmpty()) {
            return;
        }

        //从sshd的listener里面去找到pid关闭
        Set<Integer> pids = new HashSet<>();
        for (String line : sshdListenerList) {
            String pidStr = null;
            String[] strList = line.split(" ");
            for (String s : strList) {
                if (s.contains("sshd")) {
                    pidStr = s;
                    break;
                }
            }
            if (StringUtils.isEmpty(pidStr)) {
                continue;
            }
            // 如果存在/
            int lastSlashIndex = pidStr.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                // 处理/
                pidStr = pidStr.substring(0, lastSlashIndex);
            }
            try {
                pids.add(Integer.parseInt(pidStr));
            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.error("需要关闭的pid：" + pids);
        if (!pids.isEmpty()) {
            killWithPid(pids);
        }
    }

    /**
     * 一次性杀除所有的端口
     *
     * @param pids
     */
    private void killWithPid(Set<Integer> pids) {
        for (Integer pid : pids) {
            //通道连接 超时时间3s
            ChannelExec channel = null;
            try {
                channel = (ChannelExec) session.openChannel("exec");
                String command = "kill -9 " + pid;
                //开启shell通道
                //转发消息
                channel.setCommand(command);
                channel.connect(3000);
            } catch (Exception e) {
                log.error("send kill command error", e);
            } finally {
                if (channel != null) {
                    //断开连接后关闭会话
                    channel.disconnect();
                }
            }
        }
    }

    private String readTxt(InputStream in, String charset) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
