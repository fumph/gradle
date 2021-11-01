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

package org.gradle.internal.classpath

class AccessTrackingPropertiesTest extends AbstractAccessTrackingMapTest {
    @Override
    protected Properties getMapUnderTestToRead() {
        return new AccessTrackingProperties(propertiesWithContent(innerMap), consumer)
    }

    def "access to existing property with getProperty() is tracked"() {
        when:
        def returnedValue = getMapUnderTestToRead().getProperty('existing')

        then:
        returnedValue == 'existingValue'
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to existing property with getProperty() with default is tracked"() {
        when:
        def returnedValue = getMapUnderTestToRead().getProperty('existing', 'existingValue')

        then:
        returnedValue == 'existingValue'
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to missing property with getProperty() is tracked"() {
        when:
        def returnedValue = getMapUnderTestToRead().getProperty('missing')

        then:
        returnedValue == null
        1 * consumer.accept('missing', null)
        0 * consumer._
    }

    def "access to missing property with getProperty() with default is tracked"() {
        when:
        def returnedValue = getMapUnderTestToRead().getProperty('missing', 'defaultValue')

        then:
        returnedValue == 'defaultValue'
        1 * consumer.accept('missing', null)
        0 * consumer._
    }

    def "access to existing element with entrySet contains with null value is not tracked"() {
        // It is not possible to have an externally-set property with null value in the entrySet(). If the property is unset then it isn't present in the entrySet() at all.
        when:
        def result = getMapUnderTestToRead().entrySet().contains(entryWithNullValue('existing'))

        then:
        !result
        0 * consumer._
    }

    def "access to existing element with entrySet containsAll with null value is not tracked"() {
        // It is not possible to have an externally-set property with null value in the entrySet(). If the property is unset then it isn't present in the entrySet() at all.
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(entryWithNullValue('existing')))

        then:
        !result
        0 * consumer._
    }

    private static Properties propertiesWithContent(Map<String, String> contents) {
        Properties props = new Properties()
        props.putAll(contents)
        return props
    }

    private static Map.Entry<String, String> entryWithNullValue(String key) {
        // Map.entry doesn't allow null keys or values so a custom implementation is needed.
        return new Map.Entry<String, String>() {
            @Override
            String getKey() {
                return key
            }

            @Override
            String getValue() {
                return null
            }

            @Override
            String setValue(String value) {
                throw new UnsupportedOperationException()
            }
        }
    }
}
