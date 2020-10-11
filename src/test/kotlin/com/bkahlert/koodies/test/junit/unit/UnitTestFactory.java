package com.bkahlert.koodies.test.junit.unit;


import com.bkahlert.koodies.test.junit.SkipIfSystemPropertyIsTrueOrEmpty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.bkahlert.koodies.test.junit.unit.UnitTest.NAME;

/**
 * [TestFactory] variant for unit testing.<br>
 * See [UnitTest] for more details.
 * <p>
 * <b>Important: </b> This annotation must stay a Java annotation and must not be converted to a Kotlin annotation if
 * you want IntelliJ to recognize your tests. As of 2020-10-06 IntelliJ 2020-02 does not recognize Kotlin based {@link Testable}
 * annotations as tests.
 */
@SkipIfSystemPropertyIsTrueOrEmpty(NAME)
@Tag(NAME)
@TestFactory
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitTestFactory {}