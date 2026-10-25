package com.ticket.ops.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 控制台首页入口。
 * <p>
 * 根路径转发到静态页面 index.html。若不显式提供映射，Spring MVC 会把 "/" 当作静态资源
 * 处理，在缺少欢迎页时抛 NoResourceFoundException（旧版全局异常处理会误报成 500）。
 */
@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
