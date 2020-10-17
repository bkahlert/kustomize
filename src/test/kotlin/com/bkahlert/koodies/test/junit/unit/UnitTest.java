package com.bkahlert.koodies.test.junit.unit;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit 5 {@link Test} annotation to explicitly denote <a href="https://wiki.c2.com/?UnitTest)">unit tests</a>.
 * <p>
 * <b>Execution of unit tests can be deactivated for a test run using system property {@code SystemProperties.skipUnitTests}</b>.
 * <p>
 * Example: On the command line add {@code -DskipUnitTests} analogously to {@code -DskipTests}.
 * <p>
 * Hint: Combining {@code skipTests} and {@code skipUnitTests} typically makes no sense as unit tests
 * are only one kind of tests that consequently gets skipped already by the sole use of {@code skipTests}.
 * Instead you can use {@code skipIntegrationTests} which is complementary to {@code skipE2ETests}.
 * <p>
 * <b>Important: </b> This annotation must stay a Java annotation and must not be converted to a Kotlin annotation if
 * you want IntelliJ to recognize your tests. As of 2020-10-06 IntelliJ 2020-02 does not recognize Kotlin based {@link Testable}
 * annotations as tests.
 */
@Unit
@Test
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitTest {}
