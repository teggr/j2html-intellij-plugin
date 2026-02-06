package com.example.demo;

import com.example.j2htmlpreview.Preview;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

import static j2html.TagCreator.*;

/**
 * Example Java file with j2html methods for testing Phase 3 and Phase 4.
 * This file demonstrates the different types of methods that should be detected.
 * 
 * Phase 4 Test Methods:
 * - simpleComponent() - zero params, static (should execute successfully)
 * - loginForm() - zero params, static (should execute successfully)
 * - userCard() - has params (should show error message)
 * - container() - has varargs params (should show error message)
 */
public class ExampleJ2HtmlComponents {
    
    /**
     * Simple method returning ContainerTag with no parameters
     */
    public static ContainerTag simpleComponent() {
        return div("Hello World");
    }
    
    /**
     * Method with parameters returning ContainerTag
     */
    public static ContainerTag userCard(String name, String email) {
        return div()
            .withClass("user-card")
            .with(
                h2(name),
                p(email)
            );
    }
    
    /**
     * Method returning DomContent
     */
    public static DomContent loginForm() {
        return form()
            .withMethod("post")
            .with(
                input().withType("text").withName("username"),
                input().withType("password").withName("password"),
                button("Login")
            );
    }
    
    /**
     * Method returning DivTag (specific tag type)
     */
    public static DivTag container(DomContent... content) {
        return div()
            .withClass("container")
            .with(content);
    }
    
    /**
     * This method should NOT be detected (returns String, not a j2html type)
     */
    public static String getTitle() {
        return "My Page";
    }
    
    /**
     * This method should NOT be detected (returns void)
     */
    public static void printMessage(String msg) {
        System.out.println(msg);
    }
    
    /**
     * Complex method with multiple parameters
     */
    public static ContainerTag productCard(String name, double price, String imageUrl, boolean inStock) {
        return div()
            .withClass("product-card")
            .with(
                img().withSrc(imageUrl).withAlt(name),
                h3(name),
                p("$" + price),
                button(inStock ? "Add to Cart" : "Out of Stock")
                    .withCondDisabled(!inStock)
            );
    }
    
    /**
     * Preview method with friendly name - should show: "Hello World Example"
     */
    @Preview(name = "Hello World Example")
    public static ContainerTag helloWorldExample() {
        return div("Hello World from Preview!");
    }
    
    /**
     * Preview method with friendly name - should show: "Simple Login Form"
     */
    @Preview(name = "Simple Login Form")
    public static DomContent simpleLoginForm() {
        return form()
            .withMethod("post")
            .with(
                label("Username:"),
                input().withType("text").withName("username"),
                label("Password:"),
                input().withType("password").withName("password"),
                button("Login")
            );
    }
}
