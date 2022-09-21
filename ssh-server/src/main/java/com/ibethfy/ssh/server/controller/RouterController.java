package com.ibethfy.ssh.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RouterController {
    @RequestMapping("/ssh")
    public String websshpage(){
        return "webssh";
    }
}
