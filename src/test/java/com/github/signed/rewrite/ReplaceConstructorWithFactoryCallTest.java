package com.github.signed.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceConstructorWithFactoryCallTest implements RewriteTest {

    @Test
    void delegateToFactoryMethod() {
        rewriteRun(
          spec-> spec.recipe(new ReplaceConstructorWithFactoryCall(
              "org.example.createviafactory.Hello", "String",
              "org.example.createviafactory.another.HelloFactory", "createHello"
            )
          ),
          java(
            """
              package org.example.createviafactory;
              
              public class Hello {
                  private final String name;
              
                  public Hello() {
                      this("You");
                  }
              
                  public Hello(String name) {
                      this.name = name;
                  }
              }
              """),
          java(
            """
              package org.example.createviafactory.another;
              
              import org.example.createviafactory.Hello;
              
              public class HelloFactory {
              
                  public static Hello createHello(String name) {
                      return new Hello(name);
                  }
              }
              """
          ),
          java(
            """
              package org.example.createviafactory.usage;
              
              import org.example.createviafactory.Hello;
              
              public class ConstructorCall {
              
                  public void example() {
                      new Hello("Alice");
                  }
              }
              """, """
              package org.example.createviafactory.usage;
              
              import static org.example.createviafactory.another.HelloFactory.createHello;
              
              public class ConstructorCall {
              
                  public void example() {
                      createHello("Alice");
                  }
              }
              """
          )
        );
    }
}
