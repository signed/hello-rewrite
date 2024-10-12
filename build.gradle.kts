plugins {
    `maven-publish`
    id("org.openrewrite.build.recipe-library-base") version "latest.release"
    // Only needed when you want to apply the OpenRewriteBestPractices recipe to your recipes through
    // ./gradlew rewriteRun -Drewrite.activeRecipe=org.openrewrite.recipes.OpenRewriteBestPractices
    id("org.openrewrite.rewrite") version "latest.release"
}

repositories {
    mavenCentral()
}

// Set as appropriate for your organization
group = "com.github.signed"
description = "Rewrite recipes."

dependencies {
    // The bom version can also be set to a specific version
    // https://github.com/openrewrite/rewrite-recipe-bom/releases
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.20.0"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies")
    implementation("org.openrewrite:rewrite-yaml")
    implementation("org.openrewrite.meta:rewrite-analysis")
    implementation("org.assertj:assertj-core:3.26.3")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    // Refaster style recipes need the rewrite-templating annotation processor and dependency for generated recipes
    // https://github.com/openrewrite/rewrite-templating/releases
    annotationProcessor("org.openrewrite:rewrite-templating:latest.release")
    implementation("org.openrewrite:rewrite-templating")
    // The `@BeforeTemplate` and `@AfterTemplate` annotations are needed for refaster style recipes
    compileOnly("com.google.errorprone:error_prone_core:2.32.0") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.5.8")

    // Our recipe converts Guava's `Lists` type
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly("org.apache.commons:commons-lang3:latest.release")
    testRuntimeOnly("org.springframework:spring-core:latest.release")

    // Contains the OpenRewriteBestPractices recipe, which you can apply to your recipes
    rewrite("org.openrewrite.recipe:rewrite-recommendations:latest.release")
}


publishing {
    repositories {
        maven {
            name = "local"
            url = uri("http://localhost:8080/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
            isAllowInsecureProtocol = true
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.signed"
            artifactId = "hello-rewrite"
            version = "0.1.0-SNAPSHOT"
            from(components["java"])
        }
    }
}
