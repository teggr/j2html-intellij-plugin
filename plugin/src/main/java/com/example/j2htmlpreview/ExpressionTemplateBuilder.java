package com.example.j2htmlpreview;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiClass;

/**
 * Static utility class for building Java expression templates from method signatures.
 * Generates method call templates with smart default values based on parameter types.
 */
public class ExpressionTemplateBuilder {
    
    private ExpressionTemplateBuilder() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Builds an expression template for the given method with smart default parameter values.
     * Handles both static and instance method calls.
     * 
     * @param method The PSI method to build a template for
     * @return A Java expression template like "MyClass.myMethod(\"default\", 0, false)"
     */
    public static String buildExpressionTemplate(PsiMethod method) {
        if (method == null) {
            return "";
        }
        
        StringBuilder expression = new StringBuilder();
        
        // Add class name prefix for static methods
        PsiClass containingClass = method.getContainingClass();
        if (method.hasModifierProperty("static") && containingClass != null) {
            expression.append(containingClass.getName()).append(".");
        }
        
        // Add method name
        expression.append(method.getName()).append("(");
        
        // Add parameters with smart defaults
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                expression.append(", ");
            }
            expression.append(getDefaultValueForType(parameters[i].getType()));
        }
        
        expression.append(")");
        return expression.toString();
    }
    
    /**
     * Returns a smart default value for the given PSI type.
     * Supports common Java types with sensible defaults.
     */
    private static String getDefaultValueForType(PsiType type) {
        if (type == null) {
            return "null";
        }
        
        String typeName = type.getPresentableText();
        
        // Primitive types
        if (typeName.equals("int") || typeName.equals("long") || 
            typeName.equals("short") || typeName.equals("byte")) {
            return "0";
        }
        if (typeName.equals("double") || typeName.equals("float")) {
            return "0.0";
        }
        if (typeName.equals("boolean")) {
            return "false";
        }
        if (typeName.equals("char")) {
            return "'a'";
        }
        
        // Common reference types
        if (typeName.equals("String")) {
            return "\"\"";
        }
        if (typeName.startsWith("List") || typeName.equals("Collection")) {
            return "List.of()";
        }
        if (typeName.startsWith("Set")) {
            return "Set.of()";
        }
        if (typeName.startsWith("Map")) {
            return "Map.of()";
        }
        if (typeName.startsWith("Optional")) {
            return "Optional.empty()";
        }
        
        // Arrays
        if (type.getArrayDimensions() > 0) {
            return "new " + typeName + "{}";
        }
        
        // Default for other types
        return "null";
    }
    
    /**
     * Checks if a method has parameters.
     */
    public static boolean hasParameters(PsiMethod method) {
        return method != null && method.getParameterList().getParametersCount() > 0;
    }
}
