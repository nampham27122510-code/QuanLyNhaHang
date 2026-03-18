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

        // THÊM DÒNG NÀY ĐỂ TẢI THƯ VIỆN BIỂU ĐỒ
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QuanLyNhaHang"
include(":app")