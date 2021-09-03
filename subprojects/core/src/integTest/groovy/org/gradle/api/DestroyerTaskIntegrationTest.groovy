/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.file.TestFile

class DestroyerTaskIntegrationTest extends AbstractIntegrationSpec {
    BuildFixture rootBuild = new RootBuild(testDirectory)

    def "can have destroyer task depend on a task in another project"() {
        def foo = subproject(':foo')
        def bar = subproject(':bar')

        def cleanFoo = foo.task('cleanFoo').destroys('build/foo')
        def cleanBar = bar.task('cleanBar').destroys('build/bar').dependsOn(cleanFoo)
        def clean = rootBuild.task('clean').dependsOn(cleanFoo).dependsOn(cleanBar)
        def generateFoo = foo.task('generateFoo').produces('build/foo')
        def generateBar = bar.task('generateBar').produces('build/bar')
        def generate = rootBuild.task('generate').dependsOn(generateBar).dependsOn(generateFoo)

        writeAllFiles()

        when:
        args '--parallel', '--max-workers=2'
        succeeds(clean.path, generate.path)

        then:
        result.assertTaskOrder(cleanFoo.fullPath, cleanBar.fullPath, clean.fullPath)
        result.assertTaskOrder(cleanFoo.fullPath, generateFoo.fullPath, generate.fullPath)
        result.assertTaskOrder(cleanBar.fullPath, generateBar.fullPath, generate.fullPath)
    }

    def "can have destroyer task depend on a task in another build"() {
        def foo = includedBuild('child').subproject(':foo')
        def bar = subproject(':bar')

        def cleanFoo = foo.task('cleanFoo').destroys('build/foo')
        def cleanBar = bar.task('cleanBar').destroys('build/bar').dependsOn(cleanFoo)
        def clean = rootBuild.task('clean').dependsOn(cleanFoo).dependsOn(cleanBar)
        def generateFoo = foo.task('generateFoo').produces('build/foo')
        def generateBar = bar.task('generateBar').produces('build/bar')
        def generate = rootBuild.task('generate').dependsOn(generateBar).dependsOn(generateFoo)

        writeAllFiles()

        when:
        args '--parallel', '--max-workers=2'
        succeeds(clean.path, generate.path)

        then:
        result.assertTaskOrder(cleanFoo.fullPath, cleanBar.fullPath, clean.fullPath)
        result.assertTaskOrder(cleanFoo.fullPath, generateFoo.fullPath, generate.fullPath)
        result.assertTaskOrder(cleanBar.fullPath, generateBar.fullPath, generate.fullPath)
    }

    def "can order destroyer tasks after producer tasks with a dependency in another project"() {
        def foo = subproject(':foo')
        def bar = subproject(':bar')

        def cleanFoo = foo.task('cleanFoo').destroys('build/foo')
        def cleanBar = bar.task('cleanBar').destroys('build/bar')
        def clean = rootBuild.task('clean').dependsOn(cleanFoo).dependsOn(cleanBar)
        def generateFoo = foo.task('generateFoo').produces('build/foo')
        def generateBar = bar.task('generateBar').produces('build/bar').dependsOn(generateFoo)
        def generate = rootBuild.task('generate').dependsOn(generateBar).dependsOn(generateFoo)

        writeAllFiles()

        when:
        args '--parallel', '--max-workers=2'
        succeeds(generate.path, clean.path)

        then:
        result.assertTaskOrder(generateFoo.fullPath, generateBar.fullPath, generate.fullPath)
        result.assertTaskOrder(generateFoo.fullPath, cleanFoo.fullPath, clean.fullPath)
        result.assertTaskOrder(generateBar.fullPath, cleanBar.fullPath, clean.fullPath)
    }

    def "can order destroyer task after producer tasks with a dependency in another build"() {
        def foo = includedBuild('child').subproject(':foo')
        def bar = subproject(':bar')

        def cleanFoo = foo.task('cleanFoo').destroys('build/foo')
        def cleanBar = bar.task('cleanBar').destroys('build/bar')
        def clean = rootBuild.task('clean').dependsOn(cleanFoo).dependsOn(cleanBar)
        def generateFoo = foo.task('generateFoo').produces('build/foo')
        def generateBar = bar.task('generateBar').produces('build/bar').dependsOn(generateFoo)
        def generate = rootBuild.task('generate').dependsOn(generateBar).dependsOn(generateFoo)

        writeAllFiles()

        when:
        args '--parallel', '--max-workers=2'
        succeeds(generate.path, clean.path)

        then:
        result.assertTaskOrder(generateFoo.fullPath, generateBar.fullPath, generate.fullPath)
        result.assertTaskOrder(generateFoo.fullPath, cleanFoo.fullPath, clean.fullPath)
        result.assertTaskOrder(generateBar.fullPath, cleanBar.fullPath, clean.fullPath)
    }

    void writeAllFiles() {
        rootBuild.writeFiles()
    }

    ProjectFixture subproject(String path) {
        return rootBuild.subproject(path)
    }

    BuildFixture includedBuild(String name) {
        return rootBuild.includedBuild(name)
    }

    class RootBuild extends BuildFixture {
        Map<String, BuildFixture> builds = [:]

        RootBuild(TestFile rootDirectory) {
            super('root', rootDirectory)
        }

        BuildFixture includedBuild(String name) {
            return builds.get(name, new BuildFixture(name, buildDir.file(name)))
        }

        TaskFixture task(String name) {
            return subproject(':').task(name)
        }

        void writeFiles() {
            super.writeFiles()
            settingsFile << """
                ${builds.keySet().collect { "includeBuild " + quote(it) }.join('\n')}
            """.stripIndent()
            builds.values().each { it.writeFiles() }
        }
    }

    class BuildFixture {
        final String name
        final TestFile buildDir
        Map<String, ProjectFixture> projects = [:]

        BuildFixture(String name, TestFile buildDir) {
            this.name = name
            this.buildDir = buildDir
        }

        ProjectFixture subproject(String path) {
            return projects.get(path, new ProjectFixture(this, path))
        }

        void writeFiles() {
            buildDir.file('settings.gradle') << """
                rootProject.name = ${quote(name)}
                include ${projects.keySet().collect { quote(it) }.join(', ')}
            """.stripIndent()
            projects.values().each { it.writeFiles() }
        }
    }

    class ProjectFixture {
        final BuildFixture build
        final String path
        final Map<String, TaskFixture> tasks = [:]

        ProjectFixture(BuildFixture build, String path) {
            this.build = build
            this.path = path
        }

        TaskFixture task(String name) {
            return tasks.get(name, new TaskFixture(this, taskPath(name)))
        }

        String taskPath(String name) {
            return path == ':' ? ":${name}" : "${path}:${name}"
        }

        void writeFiles() {
            def projectDirPath = path.replaceAll(':', '/')
            def projectDir = build.buildDir.createDir(projectDirPath)
            projectDir.file('build.gradle') << """
                ${tasks.collect { name, task -> task.getConfig() }.join('\n') }
            """.stripIndent()
        }
    }

    class TaskFixture {
        final ProjectFixture project
        final String path
        final Set<TaskFixture> dependencies = []
        final Set<String> destroys = []
        final Set<String> produces = []

        TaskFixture(ProjectFixture project, String path) {
            this.project = project
            this.path = path
        }

        TaskFixture dependsOn(TaskFixture dependency) {
            dependencies.add(dependency)
            return this
        }

        TaskFixture destroys(String path) {
            destroys.add(path)
            return this
        }

        TaskFixture produces(String path) {
            produces.add(path)
            return this
        }

        String getName() {
            return path.split(':').last()
        }

        String getFullPath() {
            return project.build == rootBuild ? path : ":${project.build.name}${path}"
        }

        String getConfig() {
            return """
                tasks.register('${name}') {
                    ${dependencies.collect {'dependsOn ' + dependencyFor(it) }.join('\n\t\t\t\t')}
                    ${produces.collect { 'outputs.file file(' + quote(it) + ')' }.join('\n\t\t\t\t')}
                    ${destroys.collect { 'destroyables.register file(' + quote(it) + ')' }.join('\n\t\t\t\t')}
                }
            """.stripIndent()
        }

        String dependencyFor(TaskFixture task) {
            if (task.project.build == this.project.build) {
                return quote(task.path)
            } else {
                return "gradle.includedBuild(${quote(task.project.build.name)}).task(${quote(task.path)})"
            }
        }
    }

    static String quote(String text) {
        return "'${text}'"
    }
}
