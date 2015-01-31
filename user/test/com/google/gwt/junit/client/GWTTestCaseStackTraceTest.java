/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.junit.client;

import com.google.gwt.core.client.impl.DoNotInline;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.junit.client.WithProperties.Property;

import junit.framework.AssertionFailedError;

/**
 * This class tests stack traces generated by GWTTestCase.
 */
public class GWTTestCaseStackTraceTest extends GWTTestCaseTestBase {

  private static final int LINE_NUMBER_1 = 41;
  private static final int LINE_NUMBER_2 = LINE_NUMBER_1 + 2;

  private static final String FILE_NAME = "GWTTestCaseStackTraceTest.java";
  private static final String CLASS_NAME = GWTTestCaseStackTraceTest.class.getName();

  @DoNotInline
  private static void throwException(boolean withCause) {
    // the next line should be LINE_NUMBER_1
    AssertionFailedError exception = new AssertionFailedError("stack_trace_msg");
    if (withCause) {
      exception.initCause(new RuntimeException("the_cause"));
    }
    throw exception;
  }

  private static void assertStackTrace(
      Throwable t, String methodName, int lineNumber, boolean hasCause) {
    assertSame(AssertionFailedError.class, t.getClass());
    assertTrue(t.getMessage().startsWith("stack_trace_msg"));
    StackTraceElement[] trace = t.getStackTrace();
    assertStackTrace(trace, CLASS_NAME, "throwException", FILE_NAME, LINE_NUMBER_1);
    assertStackTrace(trace, CLASS_NAME, methodName, FILE_NAME, lineNumber);
    assertCause(t, hasCause);
  }

  private static void assertCause(Throwable t, boolean hasCause) {
    Throwable cause = t.getCause();
    if (hasCause) {
      assertNotNull(cause);
      assertCauseDetails(cause);
    } else {
      assertNull(cause);
    }
  }

  private static void assertCauseDetails(Throwable t) {
    assertSame(SerializableThrowable.class, t.getClass());
    String type = ((SerializableThrowable) t).getDesignatedType();
    assertEquals(RuntimeException.class.getName(), type);
    assertTrue(t.getMessage().startsWith("the_cause"));
    StackTraceElement[] trace = t.getStackTrace();
    assertStackTrace(trace, CLASS_NAME, "throwException", FILE_NAME, LINE_NUMBER_2);
  }

  private static void assertStackTrace(StackTraceElement[] stackTrace, String className,
      String methodName, String fileName, int lineNumber) {
    for (StackTraceElement stackTraceElement : stackTrace) {
      if (stackTraceElement.getClassName().equals(className)
          && stackTraceElement.getMethodName().equals(methodName)) {
        assertEquals(fileName, stackTraceElement.getFileName());
        assertEquals(lineNumber, stackTraceElement.getLineNumber());
        return; // Found!!!
      }
    }
    fail("Stack trace element not found " + className + "#" + methodName);
  }

  /** Asserts stack trace generated by {@link #testStackTrace} */
  public static class StackTraceAsserter implements ExceptionAsserter {
    public void assertException(ExpectedFailure annotation, Throwable actual) {
      final int lineNumber = 100;
      assertStackTrace(actual, "testStackTrace", lineNumber, false);
    }
  }

  @ExpectedFailure(withAsserter = StackTraceAsserter.class)
  public void testStackTrace() {
    throwException(false);
  }

  /** Asserts stack trace generated by {@link #testStackTrace_withCause} */
  public static class StackTraceAsserterWithCause implements ExceptionAsserter {
    public void assertException(ExpectedFailure annotation, Throwable actual) {
      final int lineNumber = 113;
      assertStackTrace(actual, "testStackTrace_withCause", lineNumber, true);
    }
  }

  @ExpectedFailure(withAsserter = StackTraceAsserterWithCause.class)
  public void testStackTrace_withCause() {
    throwException(true);
  }

  /** Asserts stack trace generated by {@link #testStackTrace_fromDifferentModule} */
  public static class StackTraceAsserterFromDifferentModule implements ExceptionAsserter {
    public void assertException(ExpectedFailure annotation, Throwable actual) {
      final int lineNumber = 128;
      assertStackTrace(actual, "testStackTrace_fromDifferentModule", lineNumber, false);
    }
  }

  // @Property added just to introduce a different module name for the test
  @WithProperties(@Property(name = "locale", value = "tr"))
  @ExpectedFailure(withAsserter = StackTraceAsserterFromDifferentModule.class)
  public void testStackTrace_fromDifferentModule() {
    throwException(false);
  }

  /** Asserts stack trace generated by {@link #testStackTrace_jsni} */
  public static class StackTraceAsserterForJsni implements ExceptionAsserter {
    public void assertException(ExpectedFailure annotation, Throwable actual) {
      assertEquals(1, actual.getStackTrace().length);
      StackTraceElement ste = actual.getStackTrace()[0];
      assertEquals("setUp", ste.getMethodName());
      assertEquals(FILE_NAME, ste.getFileName());
      assertEquals(9999, ste.getLineNumber());
    }
  }

  /**
   * This is asserting that {@code StacktraceDeobfuscator} trusts the stack trace emulation with the
   * file name and line number (emulation knows it better for anonymous functions in JSNI).
   */
  @ExpectedFailure(withAsserter = StackTraceAsserterForJsni.class)
  public void testStackTrace_jsni() {
    // A method name from a different file so that deobfuscator will resolve to a different file.
    String methodName = !GWT.isScript() ? "setUp"
        : Impl.getNameOf("com.google.gwt.junit.client.GWTTestCase::setUp()");
    StackTraceElement ste = new StackTraceElement("Unknown", methodName, FILE_NAME, 9999);

    AssertionFailedError exception = new AssertionFailedError("stack_trace_msg");
    exception.setStackTrace(new StackTraceElement[] {ste});
    throw exception;
  }
}
