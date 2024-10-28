package reproducer;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.UseStaticImport;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.openrewrite.java.Assertions.java;

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

    @Test
    void name() {
        final Consumer<RecipeSpec> spec = recipeSpec -> {
            recipeSpec.recipe(new UseStaticImport("org.example.createviafactory.another.HelloFactory createHello(String, long)"));
        };
        rewriteRun(spec,
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
}
