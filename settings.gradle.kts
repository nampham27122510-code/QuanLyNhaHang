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
        // DÒNG NÀY LÀ CHÌA KHÓA ĐỂ HẾT LỖI BIỂU ĐỒ
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QuanLyNhaHang"
include(":app")