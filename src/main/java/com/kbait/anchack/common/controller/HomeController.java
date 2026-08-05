package com.kbait.anchack.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("message", "Anchack Backend API");
        response.put("status", "running");

        return ResponseEntity.ok(response);
    }
}
