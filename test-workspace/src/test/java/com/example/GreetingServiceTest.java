package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    @Test
    void greetsANamedVisitor() {
        assertEquals("Hello, Ada!", service.greet("Ada").message());
    }

    @Test
    void fallsBackToWorldWhenTheNameIsBlank() {
        assertEquals("Hello, World!", service.greet("  ").message());
    }

    @Test
    void fallsBackToWorldWhenTheNameIsNull() {
        assertEquals("Hello, World!", service.greet(null).message());
    }
}
