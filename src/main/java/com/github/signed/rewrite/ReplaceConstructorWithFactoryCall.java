package com.github.signed.rewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceConstructorWithFactoryCall extends Recipe {
    private static final MethodMatcher Constructor = new MethodMatcher("org.example.createviafactory.Hello <constructor>(java.lang.String)");
    private static final MethodMatcher FactoryMethod = new MethodMatcher("org.example.createviafactory.another.HelloFactory createHello(java.lang.String)");

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Replace constructor with factory call";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Searches the code for invocations of the constructor and delegats the call to a factory method.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
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
