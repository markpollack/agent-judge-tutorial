package com.example;

/**
 * Business logic lives in a service.
 *
 * <p>Controllers in this codebase hold no logic and reach no resource directly.
 * They delegate here.
 */
public class GreetingService {

    public Greeting greet(String name) {
        if (name == null || name.isBlank()) {
            return new Greeting("Hello, World!");
        }
        return new Greeting("Hello, " + name + "!");
    }
}
