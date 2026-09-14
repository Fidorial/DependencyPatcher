plugins {
    id("com.diffplug.spotless")
}

spotless {
    format("misc") {
        target(
            "*.md",
        )
        targetExclude(
            ".gradle/**",
            ".idea/**",
            "build/**",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlin {
        ktlint("1.8.0")
        targetExclude(
            "build/**",
        )
    }

    kotlinGradle {
        ktlint("1.8.0")
        targetExclude(
            "build/**",
        )
    }
}
