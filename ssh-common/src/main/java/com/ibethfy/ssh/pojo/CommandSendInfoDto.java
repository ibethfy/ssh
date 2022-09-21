package com.ibethfy.ssh.pojo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CommandSendInfoDto {
    private String type;

    private Map<String, String> params = new HashMap<>();
}
