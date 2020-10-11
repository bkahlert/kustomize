package com.bkahlert.koodies.test.junit.e2e;


import com.bkahlert.koodies.test.junit.SkipIfSystemPropertyIsTrueOrEmpty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.bkahlert.koodies.test.junit.e2e.E2ETest.NAME;

/**
 * JUnit 5 {@link Test} annotation to explicitly denote <a href="https://wiki.c2.com/?EndToEndPrinciple)">end-to-end tests</a>.
 * <p>
 * The more the "ends" of an end-to-end test reflect the actual system border (e.g. API request vs. user pressing a button in an app)
 * the more likely test execution is not always appropriate (e.g. because they take too much time) or
 * feasible (e.g. because of different architecture).
 * <p>
 * Therefore <b>end-to-end test execution can be deactivated for a test run using system property {@code SystemProperties.skipE2ETests}</b>.
 * <p>
 * Example: On the command line add {@code -DskipE2ETests} analogously to {@code -DskipTests}.
 * <p>
 * Hint: Combining {@code skipTests} and {@code skipE2ETests} typically makes no sense as end-to-end tests
 * are only one kind of tests that consequently gets skipped already by the sole use of {@code skipTests}.
 * Instead you can use {@code skipUnitTests} which is complementary to {@code skipE2ETests}.
 * <p>
 * <b>Important: </b> This annotation must stay a Java annotation and must not be converted to a Kotlin annotation if
 * you want IntelliJ to recognize your tests. As of 2020-10-06 IntelliJ 2020-02 does not recognize Kotlin based {@link Testable}
 * annotations as tests.
 */
@SuppressWarnings("SpellCheckingInspection")
@SkipIfSystemPropertyIsTrueOrEmpty(NAME)
@Tag(NAME)
@Test
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface E2ETest {
    String NAME = "E2E";
}
