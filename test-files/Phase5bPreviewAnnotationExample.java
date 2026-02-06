package com.example.demo;

import com.example.j2htmlpreview.Preview;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;

import static j2html.TagCreator.*;

/**
 * Phase 5b test file demonstrating @Preview annotation with friendly display names.
 * This file tests the new annotation feature for better UX in the dropdown.
 */
public class Phase5bPreviewAnnotationExample {
    
    /**
     * Regular method without annotation - should show: simpleCard() → DivTag
     */
    public static DivTag simpleCard() {
        return div()
            .withClass("card")
            .with(
                h3("Simple Card"),
                p("This is a simple card without annotation")
            );
    }
    
    /**
     * Method with @Preview annotation - should show: "Bootstrap Login Form"
     */
    @Preview(name = "Bootstrap Login Form")
    public static FormTag bootstrapForm() {
        return form()
            .withClass("form-signin")
            .withMethod("post")
            .with(
                h2("Please sign in").withClass("form-signin-heading"),
                input().withType("email").withClass("form-control").withPlaceholder("Email address"),
                input().withType("password").withClass("form-control").withPlaceholder("Password"),
                button("Sign in").withClass("btn btn-lg btn-primary btn-block")
            );
    }
    
    /**
     * Method with @Preview annotation - should show: "Alice's User Card"
     */
    @Preview(name = "Alice's User Card")
    public static DivTag userCard_alice() {
        return div()
            .withClass("user-card dark-theme")
            .with(
                h2("Alice Johnson"),
                p("alice@example.com"),
                p("Software Engineer"),
                button("Follow").withClass("btn-follow")
            );
    }
    
    /**
     * Method with @Preview annotation - should show: "User Card - Long Name Edge Case"
     */
    @Preview(name = "User Card - Long Name Edge Case")
    public static DivTag userCard_longName() {
        return div()
            .withClass("user-card light-theme")
            .with(
                h2("Bartholomew Alexander Montgomery III"),
                p("bart@example.com"),
                p("Distinguished Professor of Computer Science"),
                button("Connect").withClass("btn-connect")
            );
    }
    
    /**
     * Method with @Preview annotation - should show: "Product Card - In Stock"
     */
    @Preview(name = "Product Card - In Stock")
    public static ContainerTag productCard_inStock() {
        return div()
            .withClass("product-card")
            .with(
                img().withSrc("laptop.jpg").withAlt("Laptop"),
                h3("Premium Laptop"),
                p("$1,299.99").withClass("price"),
                span("In Stock").withClass("badge-success"),
                button("Add to Cart").withClass("btn-primary")
            );
    }
    
    /**
     * Method with @Preview annotation - should show: "Product Card - Out of Stock"
     */
    @Preview(name = "Product Card - Out of Stock")
    public static ContainerTag productCard_outOfStock() {
        return div()
            .withClass("product-card disabled")
            .with(
                img().withSrc("headphones.jpg").withAlt("Headphones"),
                h3("Wireless Headphones"),
                p("$299.99").withClass("price"),
                span("Out of Stock").withClass("badge-danger"),
                button("Notify Me").withClass("btn-secondary")
            );
    }
    
    /**
     * Method with parameters (should show error when trying to execute)
     * But display name should show: "Custom User Card Template"
     */
    @Preview(name = "Custom User Card Template")
    public static DivTag customUserCard(String name, String email, String role) {
        return div()
            .withClass("user-card")
            .with(
                h2(name),
                p(email),
                p(role)
            );
    }
    
    /**
     * Regular method with parameters - should show: buildCard(String title, String content) → ContainerTag
     */
    public static ContainerTag buildCard(String title, String content) {
        return div()
            .withClass("card")
            .with(
                h3(title),
                p(content)
            );
    }
    
    /**
     * Method with empty annotation name - should fallback to method signature
     */
    @Preview(name = "")
    public static DivTag emptyNameTest() {
        return div("This has an empty annotation name");
    }
}
