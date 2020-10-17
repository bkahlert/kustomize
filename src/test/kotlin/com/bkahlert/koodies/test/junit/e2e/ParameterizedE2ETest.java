package com.bkahlert.koodies.test.junit.e2e;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link ParameterizedTest} variant for end-to-end testing.<br>
 * See {@link E2ETest} for more details.
 * <p>
 * <b>Important: </b> This annotation must stay a Java annotation and must not be converted to a Kotlin annotation if
 * you want IntelliJ to recognize your tests. As of 2020-10-06 IntelliJ 2020-02 does not recognize Kotlin based {@link Testable}
 * annotations as tests.
 */
@E2E
@ParameterizedTest
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterizedE2ETest {}
