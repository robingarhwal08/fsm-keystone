package com.fsm.keystone.controller;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<AppUser> all() {
        return userService.getAllUsers();
    }

    @GetMapping("/technicians")
    public List<AppUser> technicians() {
        return userService.getAllTechnicians();
    }
}