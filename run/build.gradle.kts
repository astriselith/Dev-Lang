plugins {
    java
    application
}

application {
    mainClass.set("com.lang.Main")
}

dependencies {
    implementation("commons-cli:commons-cli:1.10.0")
}