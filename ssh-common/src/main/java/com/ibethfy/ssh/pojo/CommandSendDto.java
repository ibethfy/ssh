package com.ibethfy.ssh.pojo;

import lombok.Data;

@Data
public class CommandSendDto {

    private String deviceId;

    private String userName;

    private String uuid;

    private CommandSendInfoDto data;
}
