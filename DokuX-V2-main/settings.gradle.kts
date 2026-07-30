pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "DocEditor"

include(
    ":app",
    ":core:model",
    ":core:layout-engine",
    ":core:command",
    ":core:gate",
    ":core:security",
    ":data:docx-engine",
    ":data:pdf-export",
    ":data:local-db",
    ":data:filesystem",
    ":data:billing",
    ":feature:editor",
    ":feature:template",
    ":feature:export",
    ":feature:tools",
    ":feature:speech-to-text",
    ":feature:file-browser",
    ":feature:ai-generate"
)
