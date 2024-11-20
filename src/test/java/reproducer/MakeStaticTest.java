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

    private final SourceSpecs helloClass = java(
      """
        package a;
        
        public class Hello {
        
            public Hello(String name, long value) {
            }
        }
        """);
    private final SourceSpecs helloFactoryClass = java(
      """
        package f;
        
        import a.Hello;
        
        public class HelloFactory {
        
            public static Hello createHello(String name, long value) {
                return new Hello(name, value);
            }
        }
        """
    );
    private final UseStaticImport makeFactoryImportStatic = new UseStaticImport("f.HelloFactory createHello(String, long)");

    @Test
    void useStaticImportWorking() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(makeFactoryImportStatic),
          helloClass,
          helloFactoryClass,
          java(
            """
              package u;
              
              import f.HelloFactory;
              
              public class ConstructorUsage {
              
                  public void example() {
                      HelloFactory.createHello("Alice", 42L);
                  }
              }
              """, """
              package u;
              
              import static f.HelloFactory.createHello;
              
              public class ConstructorUsage {
              
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
                  final MethodMatcher FactoryMethod = new MethodMatcher("f.HelloFactory createHello(String, long)");
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
                          .doBeforeParseTemplate(System.out::println)
                          .imports("f.HelloFactory")
                          .build();
                        maybeRemoveImport("a.Hello");
                        // why does this one not find the usage and needs false?
                        maybeAddImport("f.HelloFactory", false);
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
              package u;
              
              import a.Hello;
              
              public class ConstructorUsage {
              
                  public void example() {
                      new Hello("Alice", 42L);
                  }
              }
              """, """
              package u;
              
              import static f.HelloFactory.createHello;
              
              public class ConstructorUsage {
              
                  public void example() {
                      createHello("Alice", 42L);
                  }
              }
              """));
    }
}
