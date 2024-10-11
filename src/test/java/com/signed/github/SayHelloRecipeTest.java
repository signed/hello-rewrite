package com.signed.github;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SayHelloRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SayHelloRecipe("com.yourorg.FooBar"));
    }

    @Test
    void addsHelloToFooBar() {
        rewriteRun(
            java(
                """
                    package com.sample;
                
                    class FooBar {
                        public FooBar() {
                        }
                    }
                """,
                """
                    package com.sample;

                    class FooBar {
                        public FooBar(java.lang.String hello) {
                        }
                    }
                """
            )
        );
    }

    @Test
    void doesNotChangeExistingHello() {
        rewriteRun(
            java(
                """
                    package com.yourorg;
        
                    class FooBar {
                        public String hello() { return ""; }
                    }
                """
            )
        );
    }

    @Test
    void doesNotChangeOtherClasses() {
        rewriteRun(
            java(
                """
                    package com.yourorg;
        
                    class Bash {
                    }
                """
            )
        );
    }
}
