package com.signed.github;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeList;
import org.openrewrite.java.AddMethodParameter;
import org.openrewrite.java.AddNullMethodArgument;

@Value
@EqualsAndHashCode(callSuper = false)
public class SayHelloRecipe extends Recipe {
    @Option(displayName = "Fully qualified class name",
            description = "A fully qualified class name indicating which class to add a hello() method to.",
            example = "com.yourorg.FooBar")
    @NonNull
    String fullyQualifiedClassName;

    @Override
    public void buildRecipeList(RecipeList list) {
        System.out.println("running the hello script");
        list.recipe(new AddNullMethodArgument("com.sample.FooBar <constructor>()", 0, "java.lang.String", "hello", null));
        list.recipe(new AddMethodParameter("com.sample.FooBar <constructor>()", "java.lang.String", "hello", null));
    }

    // All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
    @JsonCreator
    public SayHelloRecipe(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public String getDisplayName() {
        return "Say hello";
    }

    @Override
    public String getDescription() {
        return "Adds a \"hello\" method to the specified class.";
    }

    @Override
    public String toString() {
        return "SayHelloRecipe{" +
               "fullyQualifiedClassName='" + fullyQualifiedClassName + '\'' +
               '}';
    }

}
