package com.infernokun.infernoGames.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfernoGamesRestController {

    @GetMapping
    public String index() {
        return "Welcome to Inferno Games REST API";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
