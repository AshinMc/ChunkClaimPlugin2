import os

with open('build.gradle', 'w', encoding='utf-8') as f:
    f.write('''plugins {
    id 'java'
}
group = 'org.ashin'
version = '0.5.2'

repositories {
    mavenCentral()
    maven { url = 'https://maven.enginehub.org/repo/' }
    maven { url = 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
    maven { url = 'https://oss.sonatype.org/content/groups/public/' }
}

dependencies {
    compileOnly 'org.spigotmc:spigot-api:26.1-R0.1-SNAPSHOT'
    compileOnly 'com.sk89q.worldguard:worldguard-bukkit:7.0.15'
    compileOnly 'com.sk89q.worldedit:worldedit-bukkit:7.4.0'
}

configurations.all {
    resolutionStrategy {
        force 'com.google.guava:guava:33.5.0-jre'
        force 'com.google.code.gson:gson:2.13.2'
        force 'com.google.errorprone:error_prone_annotations:2.36.0'
    }
}

def targetJavaVersion = 21
java {
    def javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

processResources {
    def props = [version: version, apiVersion: '26.1']
    inputs.properties props
    filteringCharset 'UTF-8'
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    filesMatching('plugin.yml') {
        expand props
    }
}

jar.enabled = false

['1.21', '26.1'].each { apiVer ->
    def suffix = apiVer.replace('.', '_')
    
    def procResTask = tasks.register("processResources_\", ProcessResources) {
        from sourceSets.main.resources
        into layout.buildDirectory.dir("resources/main_\")
        
        def props = [version: version, apiVersion: apiVer]
        inputs.properties props
        filteringCharset 'UTF-8'
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        
        filesMatching('plugin.yml') {
            expand props
        }
    }
    
    def buildJarTask = tasks.register("buildJar_\", Jar) {
        dependsOn procResTask
        archiveBaseName.set("ChunkClaimPlugin-\")
        from sourceSets.main.java.classesDirectory
        from procResTask.get().destinationDir
    }
    
    tasks.named('build').configure {
        dependsOn buildJarTask
    }
}
''')
