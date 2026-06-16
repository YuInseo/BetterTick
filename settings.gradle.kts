pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // KakaoMaps SDK v2 — keep both the current Nexus 3 path and the
        // legacy public group as a fallback so artifact resolution succeeds
        // regardless of which host serves the pinned version.
        maven { url = uri("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/") }
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}

rootProject.name = "BetterTick"
include(":app")
