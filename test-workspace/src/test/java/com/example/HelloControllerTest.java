package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    private final HelloController controller = new HelloController(new GreetingService());

    @Test
    void delegatesToTheService() {
        assertEquals(new Greeting("Hello, Grace!"), controller.hello("Grace"));
    }
}
