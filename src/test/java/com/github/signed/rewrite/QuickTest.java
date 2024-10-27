package com.github.signed.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class QuickTest implements RewriteTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/1030")
    @Test
    void addStaticImportForReferencedField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    if (!classDecl.getBody().getStatements().isEmpty()) {
                        return classDecl;
                    }

                    maybeAddImport("java.time.temporal.ChronoUnit");
                    maybeAddImport("java.time.temporal.ChronoUnit", "MILLIS");

                    return JavaTemplate.builder("ChronoUnit unit = MILLIS;")
                      .contextSensitive()
                      .imports("java.time.temporal.ChronoUnit")
                      .staticImports("java.time.temporal.ChronoUnit.MILLIS")
                      .build()
                      .apply(getCursor(), classDecl.getBody().getCoordinates().lastStatement());
                }
            }
          )),
          java(
            """
              public class A {
                          
              }
              """,
            """
              import java.time.temporal.ChronoUnit;
                          
              import static java.time.temporal.ChronoUnit.MILLIS;
                          
              public class A {
                  ChronoUnit unit = MILLIS;
                          
              }
              """
          )
        );
    }
}
