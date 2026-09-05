package com.example;

/**
 * A response body.
 *
 * <p>Controllers in this codebase return a record, never a raw Map and never a
 * hand-built String.
 */
public record Greeting(String message) {
}
