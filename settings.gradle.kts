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
        // KakaoMaps SDK v2 — current official Nexus 3 path. The legacy
        // `nexus/content/groups/public/` URL is deprecated; the docs now point
        // to `nexus/repository/kakaomap-releases/`.
        maven { url = uri("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/") }
    }
}

rootProject.name = "BetterTick"
include(":app")
