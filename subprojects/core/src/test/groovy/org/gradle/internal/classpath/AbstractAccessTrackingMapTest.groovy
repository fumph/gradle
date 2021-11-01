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

import spock.lang.Specification

import java.util.function.BiConsumer

abstract class AbstractAccessTrackingMapTest extends Specification {
    protected final Map<String, String> innerMap = ['existing': 'existingValue', 'other': 'otherValue']
    protected final BiConsumer<Object, Object> consumer = Mock()

    protected abstract Map<? super String, ? super String> getMapUnderTestToRead()

    def "access to existing element with get() is tracked"() {
        when:
        def result = getMapUnderTestToRead().get('existing')

        then:
        result == 'existingValue'
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to missing element with get() is tracked"() {
        when:
        def result = getMapUnderTestToRead().get('missing')

        then:
        result == null
        1 * consumer.accept('missing', null)
        0 * consumer._
    }

    def "access to existing element with getOrDefault() is tracked"() {
        when:
        def result = getMapUnderTestToRead().getOrDefault('existing', 'defaultValue')

        then:
        result == 'existingValue'
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to missing element with getOrDefault() is tracked"() {
        when:
        def result = getMapUnderTestToRead().getOrDefault('missing', 'defaultValue')

        then:
        result == 'defaultValue'
        1 * consumer.accept('missing', null)
        0 * consumer._
    }

    def "access to existing element with forEach() is tracked"() {
        when:
        def iterated = new HashMap<Object, Object>()
        getMapUnderTestToRead().forEach { k, v -> iterated.put(k, v) }

        then:
        iterated == innerMap
        1 * consumer.accept('existing', 'existingValue')
        1 * consumer.accept('other', 'otherValue')
        0 * consumer._
    }

    def "access to existing element with entrySet() is tracked"() {
        when:
        def result = Set.copyOf(getMapUnderTestToRead().entrySet())

        then:
        result == innerMap.entrySet()
        1 * consumer.accept('existing', 'existingValue')
        1 * consumer.accept('other', 'otherValue')
        0 * consumer._
    }


    def "access to existing element with entrySet contains is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().contains(Map.entry('existing', 'existingValue'))

        then:
        result
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to existing element with entrySet contains with different value is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().contains(Map.entry('existing', 'otherValue'))

        then:
        !result
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to missing element with entrySet contains is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().contains(Map.entry('missing', 'someValue'))

        then:
        !result
        1 * consumer.accept('missing', null)
        0 * consumer._
    }

    def "access to existing element with entrySet containsAll is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(Map.entry('existing', 'existingValue')))

        then:
        result
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to multiple existing elements with entrySet containsAll is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(Map.entry('existing', 'existingValue'), Map.entry('other', 'otherValue')))

        then:
        result
        1 * consumer.accept('existing', 'existingValue')
        1 * consumer.accept('other', 'otherValue')
        0 * consumer._
    }

    def "access to existing element with entrySet containsAll with different value is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(Map.entry('existing', 'otherValue')))

        then:
        !result
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to multiple existing elements with entrySet containsAll with different value is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(Map.entry('existing', 'otherValue'), Map.entry('other', 'otherValue')))

        then:
        !result
        1 * consumer.accept('existing', 'existingValue')
        1 * consumer.accept('other', 'otherValue')
        0 * consumer._
    }

    def "access to missing element with entrySet containsAll is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(Map.entry('missing', 'someValue')))

        then:
        !result
        1 * consumer.accept('missing', null)
        0 * consumer._
    }

    def "access to missing and existing elements with entrySet containsAll is tracked"() {
        when:
        def result = getMapUnderTestToRead().entrySet().containsAll(Set.of(Map.entry('missing', 'someValue'), Map.entry('existing', 'existingValue')))

        then:
        !result
        1 * consumer.accept('missing', null)
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to existing element with containsKey is tracked"() {
        when:
        def result = getMapUnderTestToRead().containsKey('existing')

        then:
        result
        1 * consumer.accept('existing', 'existingValue')
        0 * consumer._
    }

    def "access to missing element with containsKey is tracked"() {
        when:
        def result = getMapUnderTestToRead().containsKey('missing')

        then:
        !result
        1 * consumer.accept('missing', null)
        0 * consumer._
    }
}
