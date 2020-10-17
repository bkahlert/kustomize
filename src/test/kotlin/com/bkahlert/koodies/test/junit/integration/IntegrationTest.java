package com.bkahlert.koodies.test.junit.integration;


import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit 5 {@link Test} annotation to explicitly denote <a href="https://wiki.c2.com/?IntegrationTest)">integration tests</a>.
 * <p>
 * <b>Execution of integration tests can be deactivated for a test run using system property {@code SystemProperties.skipIntegrationTests}</b>.
 * <p>
 * Example: On the command line add {@code -DskipIntegrationTests} analogously to {@code -DskipTests} or {@code -DskipE2ETests}.
 * <p>
 * Hint: Combining {@code skipTests} and {@code skipIntegrationTests} typically makes no sense as integration tests
 * are only one kind of tests that consequently gets skipped already by the sole use of {@code skipTests}.
 * Instead you can use {@code skipUnitTests} and {@code skipE2ETests} which cover all tests but the integration tests.
 * <p>
 * <b>Important: </b> This annotation must stay a Java annotation and must not be converted to a Kotlin annotation if
 * you want IntelliJ to recognize your tests. As of 2020-10-06 IntelliJ 2020-02 does not recognize Kotlin based {@link Testable}
 * annotations as tests.
 */
@SuppressWarnings("SpellCheckingInspection")
@Integration
@Test
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationTest {}
