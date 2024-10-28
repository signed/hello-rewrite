# Recipe Sources

[Amazon SDK](https://github.com/aws/aws-sdk-java-v2/blob/105d5457c76f6965c3a0453b09be0e63e9c0ffea/v2-migration/src/main/java/software/amazon/awssdk/v2migration/NewClassToStaticFactory.java)

## Applying OpenRewrite recipe development best practices

We maintain a collection of [best practices for writing OpenRewrite recipes](https://docs.openrewrite.org/recipes/recipes/openrewritebestpractices).
You can apply these recommendations to your recipes by running the following command:

```bash
./gradlew rewriteRun -Drewrite.activeRecipe=org.openrewrite.recipes.OpenRewriteBestPractices
```


# Helpful
 
- [Visiting Tree printer](https://docs.openrewrite.org/concepts-and-explanations/tree-visiting-printer)

# Office Hours

## [JavaTemplate deep dive and debugging](https://youtu.be/OB_tqS356qU?t=826)
- doBeforeParseTemplate() and sout the content to see the entire generated code
- template values #{any(java.util.Collection)}
- Cursor messaging, share data inside a compilation unit from deeper visit methods to visit methods higher in the LST
- Scanning Recipes, to share data across compilation units
- Execution Context, can also share data across compilation units (don't do it)

## [Recipe authoring tips and tricks](https://youtu.be/qKbUM5lKjPE?t=254)
- Composing Recipes
- working with raw parsers if there is no JavaTemplate
