package de.weltn24.gradle.plugins

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy

class SpringBootConventionsPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.apply(plugin: 'java')

        project.repositories {
            jcenter()
        }

        final SpringBootConventionsPluginExtension pluginVariables = project.extensions.create('weltn24SpringBootConventions', SpringBootConventionsPluginExtension)

        project.afterEvaluate {
            project.dependencies.add(JavaPlugin.COMPILE_CONFIGURATION_NAME, "org.springframework.boot:spring-boot-starter-actuator")
            project.dependencies.add(JavaPlugin.COMPILE_CONFIGURATION_NAME, "org.springframework.boot:spring-boot-devtools")
            project.dependencies.add(JavaPlugin.COMPILE_CONFIGURATION_NAME, "org.springframework.boot:spring-boot-starter-security")
            project.dependencies.add(JavaPlugin.RUNTIME_CONFIGURATION_NAME, "org.jolokia:jolokia-core")
            project.dependencies.add(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, "org.springframework.boot:spring-boot-starter-test")
        }

        project.task('generateBuildProperties',
            type: Copy,
            group: 'build',
            description: 'Generates build properties file.') {


            from('src/main/resources/config') {
                include 'application.yml'
                expand(project.properties)

            }
            duplicatesStrategy = 'include'
            into 'build/resources/main/config'
        }
        project.tasks.findByName('processResources').finalizedBy('generateBuildProperties')
        project.tasks.findByName('generateBuildProperties').onlyIf { project.file("src/main/resources/config/application.yml").exists() }

        project.task('generateGitProperties',
                group: 'build',
                description: 'Generates Git properties file.') << {

            try {
                def repo = Grgit.open(project.file(project.rootDir))
                def dir = new File(project.buildDir, "resources/main")
                def file = new File(project.buildDir, "resources/main/git.properties")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (!file.exists()) {
                    file.createNewFile()
                }
                def map = ["git.branch"                : repo.branch.current.name
                           , "git.commit.id"           : repo.head().id
                           , "git.commit.id.abbrev"    : repo.head().abbreviatedId
                           , "git.commit.user.name"    : repo.head().author.name
                           , "git.commit.user.email"   : repo.head().author.email
                           , "git.commit.message.short": repo.head().shortMessage
                           , "git.commit.message.full" : repo.head().fullMessage
                           , "git.commit.time"         : repo.head().time.toString()]
                def props = new Properties()
                props.putAll(map)
                props.store(file.newWriter(), "")
            } catch (Exception ex) {
                project.logger.warn "Git repository not found, skipping generation of git.properties"
            }
        }
        project.tasks.findByName('generateGitProperties').onlyIf { pluginVariables.generateGitProperties }
        project.tasks.findByName('classes').dependsOn('generateGitProperties')

    }
}
