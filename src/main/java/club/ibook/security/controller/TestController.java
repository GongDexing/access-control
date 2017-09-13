package club.ibook.security.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import club.ibook.security.annotation.UserRole;

@RestController
@RequestMapping(path = "/test")
public class TestController {

    @UserRole(name = {"admin"})
    @RequestMapping(path = "/admin", method = RequestMethod.GET)
    public String test() {
        return "test";
    }

    @UserRole(name = {"reader"})
    @RequestMapping(path = "/reader", method = RequestMethod.GET)
    public String good() {
        return "good";
    }

    @RequestMapping(path = "/no", method = RequestMethod.GET)
    public String no() {
        return "no";
    }
}
