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

class IsolatedProjectsAccessFromKotlinDslIntegrationTest extends AbstractIsolatedProjectsIntegrationTest {
    @Unroll
    def "reports problem when build script uses #block block to apply plugins to another project"() {
        settingsFile << """
            include("a")
            include("b")
        """
        buildKotlinFile << """
            $block {
                plugins.apply("java-library")
            }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle.kts': Cannot access project ':a' from project ':'")
            problem("Build file 'build.gradle.kts': Cannot access project ':b' from project ':'")
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
        buildKotlinFile << """
            plugins { id("java-library") }
            $block {
                plugins.apply("java-library")
                java { }
                java.sourceCompatibility
            }
        """

        when:
        configurationCacheFails("assemble")

        then:
        fixture.assertStateStoreFailed {
            projectsConfigured(":", ":a", ":b")
            problem("Build file 'build.gradle.kts': Cannot access project ':a' from project ':'", 3)
            problem("Build file 'build.gradle.kts': Cannot access project ':b' from project ':'", 3)
        }

        where:
        block         | _
        "allprojects" | _
        "subprojects" | _
    }
}
