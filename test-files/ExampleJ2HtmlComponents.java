package com.example.demo;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

/**
 * Example Java file with j2html methods for testing Phase 3.
 * This file demonstrates the different types of methods that should be detected.
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
}
