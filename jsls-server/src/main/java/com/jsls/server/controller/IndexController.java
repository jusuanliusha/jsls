package com.jsls.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jsls.core.Result;

@Controller
@RequestMapping("/")
public class IndexController {

    @RequestMapping("test")
    @ResponseBody
    public Result<Void> test() {
        return Result.SUCCESS;
    }

    @RequestMapping("")
    @ResponseBody
    public Result<String> index() {
        return Result.success("welcome use jsls framework");
    }
}
