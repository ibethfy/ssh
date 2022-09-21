package com.ibethfy.ssh.server.pojo;

import lombok.Data;

@Data
public class WebSSHData {
    //操作
    private String operate;
    private String host;
    //端口号默认为22
    private Integer port = 22;
    private String username;
    private String password;
    /**
     * 类型，1为正向SSH，2为反向SSH，默认为2
     */
    private Integer type = 2;
    private String deviceId;
    private String command = "";
}
