package com.example.j2htmlpreview;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a j2html component preview with a friendly display name.
 * 
 * Preview methods should be static, return a j2html type (ContainerTag, DomContent, etc.),
 * and typically have no parameters (self-contained examples).
 * 
 * Example:
 * <pre>
 * &#64;Preview(name = "Bootstrap Login Form")
 * public static ContainerTag loginForm_example() {
 *     return loginForm(true, "Invalid password");
 * }
 * </pre>
 * 
 * The annotation can be placed in main source code or test source code.
 * Future: Can be stripped from production builds via Maven/Gradle plugin.
 */
@Retention(RetentionPolicy.RUNTIME)  // Available at runtime for reflection
@Target(ElementType.METHOD)           // Can only be applied to methods
public @interface Preview {
    /**
     * Friendly display name for this preview.
     * Shown in the preview dropdown instead of the method name.
     */
    String name();
    
    /**
     * Optional description of what this preview demonstrates.
     * Reserved for future use (documentation generation).
     */
    String description() default "";
    
    /**
     * Optional tags for categorization.
     * Reserved for future use (filtering, grouping).
     */
    String[] tags() default {};
}
