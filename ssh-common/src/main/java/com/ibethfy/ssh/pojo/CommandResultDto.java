package com.ibethfy.ssh.pojo;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class CommandResultDto {
    private String deviceId;

    private String uuid;

    private String commandType;

    private String result;

    private String message;
}
