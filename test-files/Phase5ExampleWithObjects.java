package com.example.demo;

import j2html.tags.ContainerTag;
import j2html.tags.specialized.DivTag;

import static j2html.TagCreator.*;

/**
 * Phase 5 test file demonstrating expression evaluation with object construction.
 * Tests the JavaCompiler API integration for complex expressions.
 */
public class Phase5ExampleWithObjects {
    
    /**
     * Simple User class for testing object construction in expressions
     */
    public static class User {
        public final String name;
        public final String email;
        
        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
    
    /**
     * Method that takes a User object as parameter.
     * Expression to test: userCard(new User("Alice", "alice@example.com"))
     */
    public static DivTag userCard(User user) {
        return div()
            .withClass("user-card")
            .with(
                h2(user.name),
                p(user.email),
                p("User profile")
            );
    }
    
    /**
     * Method with User and theme parameter.
     * Expression to test: userCard(new User("Bob", "bob@example.com"), "dark")
     */
    public static DivTag userCard(User user, String theme) {
        return div()
            .withClass("user-card " + theme)
            .with(
                h2(user.name),
                p(user.email),
                p("Theme: " + theme)
            );
    }
    
    /**
     * Method that calls other static methods.
     * Expression to test: userCard(createUser("Charlie"))
     */
    public static User createUser(String name) {
        return new User(name, name.toLowerCase() + "@example.com");
    }
    
    /**
     * Complex multi-parameter method.
     * Expression to test: productDisplay("Laptop", 999.99, true)
     */
    public static ContainerTag productDisplay(String name, double price, boolean inStock) {
        return div()
            .withClass("product")
            .with(
                h3(name),
                p("$" + price),
                p(inStock ? "In Stock" : "Out of Stock")
            );
    }
    
    /**
     * Method demonstrating multi-line expressions.
     * Expression to test:
     * userCard(
     *     new User(
     *         "Diana",
     *         "diana@example.com"
     *     ),
     *     "light"
     * )
     */
    public static DivTag richUserCard(User user, String theme, int followers) {
        return div()
            .withClass("user-card-rich " + theme)
            .with(
                h2(user.name),
                p(user.email),
                p(followers + " followers"),
                button("Follow")
            );
    }
}
