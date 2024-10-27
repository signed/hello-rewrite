# Recipe Sources

[Amazon SDK](https://github.com/aws/aws-sdk-java-v2/blob/105d5457c76f6965c3a0453b09be0e63e9c0ffea/v2-migration/src/main/java/software/amazon/awssdk/v2migration/NewClassToStaticFactory.java)

## Applying OpenRewrite recipe development best practices

We maintain a collection of [best practices for writing OpenRewrite recipes](https://docs.openrewrite.org/recipes/recipes/openrewritebestpractices).
You can apply these recommendations to your recipes by running the following command:

```bash
./gradlew rewriteRun -Drewrite.activeRecipe=org.openrewrite.recipes.OpenRewriteBestPractices
```
