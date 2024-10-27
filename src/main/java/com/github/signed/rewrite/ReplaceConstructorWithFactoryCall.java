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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceConstructorWithFactoryCall extends Recipe {
    @Option(displayName = "Class name",
            example = "java.math.BigDecimal")
    @NonNull
    String className;

    @Option(displayName = "Constructor signature",
            example = "long")
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
        final int numberOfConstructorParameter = constructorSignature.split(",").length;
        final MethodMatcher Constructor = new MethodMatcher(className + " <constructor>(" + constructorSignature+ ")");
        final MethodMatcher FactoryMethod = new MethodMatcher(factoryClassName + " " + factoryMethodName + "(" + constructorSignature + ")");

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

                final String constructorSignatureTemplate = Collections.nCopies(numberOfConstructorParameter, "#{any()}").stream().collect(Collectors.joining(", ", "(",")"));

                JavaTemplate factoryCall = JavaTemplate.builder(factoryMethodName + constructorSignatureTemplate)
                        .javaParser(JavaParser.fromJavaVersion().dependsOn("            " +
                                                                           "              package org.example.createviafactory.another;\n" +
                                                                           "              \n" +
                                                                           "              import org.example.createviafactory.Hello;\n" +
                                                                           "              \n" +
                                                                           "              public class HelloFactory {\n" +
                                                                           "              \n" +
                                                                           "                  public static Hello " + factoryMethodName + "(String name) {\n" +
                                                                           "                      return new Hello(name);\n" +
                                                                           "                  }\n" +
                                                                           "              }\n" +
                                                                           "              "))
                        .staticImports(factoryClassName + "." + factoryMethodName)
                        .build();

                maybeRemoveImport(className);
                maybeAddImport(factoryClassName, factoryMethodName);
                final List<Expression> arguments = newClass.getArguments();
                return factoryCall.apply(getCursor(), newClass.getCoordinates().replace(), (Object[]) arguments.toArray(new Expression[0]));
            }
        };
    }
}
