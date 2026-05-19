package com.example;

import java.util.Map;

/**
 * Sample REST controller for judge evaluation.
 * This file exists in the test workspace so judges can verify
 * file existence, content, and annotations.
 */
public class HelloController {

    public Map<String, String> hello() {
        return Map.of("message", "Hello, World!");
    }
}
