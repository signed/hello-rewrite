package com.github.signed.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.openrewrite.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceConstructorWithFactoryCall extends Recipe {
    @Option(displayName = "Class name",
            example = "java.math.BigDecimal")
    @NonNull
    String className;

    @Option(displayName = "Constructor signature",
            example = "(long)")
    @NonNull
    String constructorSignature;

    @Option(displayName = "Factory class name",
            example = "java.math.BigDecimal")
    @NonNull
    String factoryClassName;

    @Option(displayName = "New field target",
            example = "valueOf")
    @NonNull
    String factoryMethodName;


    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Replace constructor with factory call";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Searches the code for invocations of the constructor and delegats the call to a factory method.";
    }

    @JsonCreator
    public ReplaceConstructorWithFactoryCall(
            @NonNull @JsonProperty("className") String className,
            @NonNull @JsonProperty("constructorSignature") String constructorSignature,
            @NonNull @JsonProperty("factoryClassName") String factoryClassName,
            @NonNull @JsonProperty("factoryMethodName") String factoryMethodName) {
        this.className = className;
        this.constructorSignature = constructorSignature;
        this.factoryClassName = factoryClassName;
        this.factoryMethodName = factoryMethodName;
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher Constructor = new MethodMatcher("org.example.createviafactory.Hello <constructor>(java.lang.String)");
        final MethodMatcher FactoryMethod = new MethodMatcher("org.example.createviafactory.another.HelloFactory createHello(java.lang.String)");

        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                final J result = super.visitNewClass(newClass, executionContext);

                if (!Constructor.matches(newClass)) {
                    return result;
                }
                final Cursor parent = getCursor().getParentOrThrow();
                final J.MethodDeclaration methodDeclaration = parent.firstEnclosing(J.MethodDeclaration.class);
                final J.ClassDeclaration classDeclaration = parent.firstEnclosing(J.ClassDeclaration.class);
                if (FactoryMethod.matches(methodDeclaration, classDeclaration)) {
                    return result;
                }

                JavaTemplate factoryCall = JavaTemplate.builder("createHello(#{any()})")
                        .javaParser(JavaParser.fromJavaVersion().dependsOn("            " +
                                                                           "              package org.example.createviafactory.another;\n" +
                                                                           "              \n" +
                                                                           "              import org.example.createviafactory.Hello;\n" +
                                                                           "              \n" +
                                                                           "              public class HelloFactory {\n" +
                                                                           "              \n" +
                                                                           "                  public static Hello createHello(String name) {\n" +
                                                                           "                      return new Hello(name);\n" +
                                                                           "                  }\n" +
                                                                           "              }\n" +
                                                                           "              "))
                        .staticImports("org.example.createviafactory.another.HelloFactory.createHello")
                        .build();

                maybeRemoveImport("org.example.createviafactory.Hello");
                maybeAddImport("org.example.createviafactory.another.HelloFactory", "createHello");
                return factoryCall.apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().get(0));
            }
        };
    }
}
