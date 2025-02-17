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

package org.gradle.configurationcache.isolated


import spock.lang.Unroll

class IsolatedProjectsAccessFromGroovyDslIntegrationTest extends AbstractIsolatedProjectsIntegrationTest {
    @Unroll
    def "reports problem when build script uses #block block to apply plugins to another project"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildFile << """
            $block {
                plugins.apply('java-library')
            }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle': Cannot access project ':a' from project ':'")
            problem("Build file 'build.gradle': Cannot access project ':b' from project ':'")
        }

        where:
        block         | _
        "allprojects" | _
        "subprojects" | _
    }

    @Unroll
    def "reports problem when build script uses #block block to access dynamically added elements"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildFile << """
            $block {
                plugins.apply('java-library')
                java { }
                java.sourceCompatibility
            }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle': Cannot access project ':a' from project ':'", 3)
            problem("Build file 'build.gradle': Cannot access project ':b' from project ':'", 3)
        }

        where:
        block         | _
        "allprojects" | _
        "subprojects" | _
    }

    @Unroll
    def "reports problem when build script uses #property property to apply plugins to another project"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildFile << """
            ${property}.each {
                it.plugins.apply('java-library')
            }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle': Cannot access project ':a' from project ':'")
            problem("Build file 'build.gradle': Cannot access project ':b' from project ':'")
        }

        where:
        property      | _
        "allprojects" | _
        "subprojects" | _
    }

    def "reports problem when build script uses project() block to apply plugins to another project"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildFile << """
            project(':a') {
                plugins.apply('java-library')
            }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle': Cannot access project ':a' from project ':'")
        }
    }

    @Unroll
    def "reports problem when build script uses #method method to apply plugins to another project"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildFile << """
            $method(':a').plugins.apply('java-library')
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle': Cannot access project ':a' from project ':'")
        }

        where:
        method        | _
        "project"     | _
        "findProject" | _
    }

    @Unroll
    def "reports problem when root project build script uses chain of methods #chain { } to apply plugins to other projects"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildFile << """
            $chain { it.plugins.apply('java-library') }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle': Cannot access project ':a' from project ':'")
            problem("Build file 'build.gradle': Cannot access project ':b' from project ':'")
        }

        where:
        chain                                           | _
        "project(':').allprojects"                      | _
        "project(':').subprojects"                      | _
        "project('b').project(':').allprojects"         | _
        "project('b').project(':').subprojects"         | _
        "project(':').allprojects.each"                 | _
        "project(':').subprojects.each"                 | _
        "project('b').project(':').allprojects.each"    | _
        "project('b').project(':').subprojects.each"    | _
        "findProject('b').findProject(':').subprojects" | _
    }

    @Unroll
    def "reports problem when project build script uses chain of methods #chain { } to apply plugins to sibling projects"() {
        settingsFile << """
            include("a")
            include("b")
        """
        file("a/build.gradle") << """
            $chain { it.plugins.apply('java-library') }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'a${File.separator}build.gradle': Cannot access project ':b' from project ':a'")
        }

        where:
        chain                                    | _
        "project(':').subprojects"               | _
        "project(':').subprojects.each"          | _
        "project(':b').project(':').subprojects" | _
        "project(':').project('b')"              | _
        "findProject(':').findProject('b').with" | _
    }

    @Unroll
    def "reports problem when project build script uses chain of methods #chain { } to apply plugins to all projects"() {
        settingsFile << """
            include("a")
            include("b")
        """
        file("a/build.gradle") << """
            $chain { it.plugins.apply('java-library') }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'a${File.separator}build.gradle': Cannot access project ':' from project ':a'")
            problem("Build file 'a${File.separator}build.gradle': Cannot access project ':b' from project ':a'")
        }

        where:
        chain                           | _
        "project(':').allprojects"      | _
        "project(':').allprojects.each" | _
    }

    def "build script can query basic details of projects in allprojects block"() {
        settingsFile << """
            rootProject.name = "root"
            include("a")
            include("b")
        """
        buildFile << """
            plugins {
                id('java-library')
            }
            allprojects {
                println("project name = " + name)
                println("project path = " + path)
                println("project projectDir = " + projectDir)
                println("project rootDir = " + rootDir)
                it.name
                project.name
                project.path
                allprojects { }
            }
        """

        when:
        configurationCacheRun("assemble")

        then:
        fixture.assertStateStored {
            projectsConfigured(":", ":a", ":b")
        }
        outputContains("project name = root")
        outputContains("project name = a")
        outputContains("project name = b")
    }
}
