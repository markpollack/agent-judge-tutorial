package com.example;

/**
 * The idiomatic controller in this codebase.
 *
 * <p>It holds no logic, opens no resource, and returns a record. Every other
 * controller here is expected to look like this one. That expectation is the
 * convention a judge can be asked about; it is not written down anywhere a
 * compiler can read.
 */
public class HelloController {

    private final GreetingService greetings;

    public HelloController(GreetingService greetings) {
        this.greetings = greetings;
    }

    public Greeting hello(String name) {
        return greetings.greet(name);
    }
}
