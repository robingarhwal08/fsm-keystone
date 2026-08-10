package com.fsm.keystone.architecture.fixtures.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synthetic controller fixture that has a Spring mapping annotation but NO @PreAuthorize.
 * Used by LayerRulesTest to verify that the annotation-coverage rule flags this method.
 * This is a test fixture — it is never deployed.
 */
@RestController
@RequestMapping("/synthetic")
public class HandlerWithoutPreAuthorize {

    @GetMapping("/no-auth")
    public String noAuth() {
        return "no @PreAuthorize — expect annotation-coverage violation";
    }
}
