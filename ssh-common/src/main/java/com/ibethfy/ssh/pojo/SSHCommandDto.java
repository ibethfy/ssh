package com.ibethfy.ssh.pojo;

import lombok.Data;

@Data
public class SSHCommandDto {

    private String deviceId;

    private Integer serverPort;

    private String serverAddress;

    private String serverUserName;

    private String serverPassword;

    private String uuid;
}
