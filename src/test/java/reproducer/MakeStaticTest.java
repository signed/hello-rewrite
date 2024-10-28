package reproducer;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.UseStaticImport;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class MakeStaticTest implements RewriteTest {

    private SourceSpecs helloClass = java(
      """
        package org.example.createviafactory;
        
        public class Hello {
        
            public Hello(String name, long value) {
            }
        }
        """);
    private SourceSpecs helloFactoryClass = java(
      """
        package org.example.createviafactory.another;
        
        import org.example.createviafactory.Hello;
        
        public class HelloFactory {
        
            public static Hello createHello(String name, long value) {
                return new Hello(name, value);
            }
        }
        """
    );
    private UseStaticImport makeFactoryImportStatic = new UseStaticImport("org.example.createviafactory.another.HelloFactory createHello(String, long)");

    @Test
    void name() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(makeFactoryImportStatic),
          helloClass,
          helloFactoryClass,
          java(
            """
              package org.example.createviafactory.usage;
              
              import org.example.createviafactory.another.HelloFactory;
              
              public class ConstructorCall {
              
                  public void example() {
                      HelloFactory.createHello("Alice", 42L);
                  }
              }
              """, """
              package org.example.createviafactory.usage;
              
              import static org.example.createviafactory.another.HelloFactory.createHello;
              
              public class ConstructorCall {
              
                  public void example() {
                      createHello("Alice", 42L);
                  }
              }
              """));
    }

    @Test
    void noStaticImportAdded() {
        rewriteRun(recipeSpec -> {
              recipeSpec.recipe(toRecipe(() -> new JavaVisitor<>() {
                  MethodMatcher FactoryMethod = new MethodMatcher("org.example.createviafactory.another.HelloFactory createHello(String, long)");
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                        final var result = (J.NewClass) super.visitNewClass(newClass, executionContext);
                        final Cursor parent = getCursor().getParentOrThrow();
                        final J.MethodDeclaration methodDeclaration = parent.firstEnclosing(J.MethodDeclaration.class);
                        final J.ClassDeclaration classDeclaration = parent.firstEnclosing(J.ClassDeclaration.class);
                        final var isInstantiationInFactoryMethod = FactoryMethod.matches(methodDeclaration, classDeclaration);
                        if (isInstantiationInFactoryMethod) {
                            return result;
                        }

                        final var template = JavaTemplate.builder("HelloFactory.createHello(#{any()}, #{any()})")
                          .doBeforeParseTemplate((code)-> System.out.println(code))
                          .imports("org.example.createviafactory.another.HelloFactory")
                          .build();
                        maybeRemoveImport("org.example.createviafactory.Hello");
                        // why does this one not find the usage and needs false?
                        maybeAddImport("org.example.createviafactory.another.HelloFactory", false);
                        // you are also having the cache problem?
                        doAfterVisit(makeFactoryImportStatic.getVisitor());

                        final var arguments = result.getArguments();
                        return template.apply(getCursor(), result.getCoordinates().replace(), arguments.get(0), arguments.get(1));
                    }
                }
              ));
          },
          helloClass,
          helloFactoryClass,
          java(
            """
              package org.example.createviafactory.usage;
              
              import org.example.createviafactory.Hello;
              
              public class ConstructorCall {
              
                  public void example() {
                      new Hello("Alice", 42L);
                  }
              }
              """, """
              package org.example.createviafactory.usage;
              
              import static org.example.createviafactory.another.HelloFactory.createHello;
              
              public class ConstructorCall {
              
                  public void example() {
                      createHello("Alice", 42L);
                  }
              }
              """));
    }
}
