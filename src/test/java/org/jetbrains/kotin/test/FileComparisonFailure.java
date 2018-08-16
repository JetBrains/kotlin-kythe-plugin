package org.jetbrains.kotin.test;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;

public class FileComparisonFailure extends ComparisonFailure {
  private final String myMessage;
  private final String myExpected;
  private final String myActual;
  private final String myFilePath;
  private final String myActualFilePath;

  public FileComparisonFailure(String message, /*@NotNull */String expected, /*@NotNull */String actual, String expectedFilePath) {
    this(message, expected, actual, expectedFilePath, null);
  }

  public FileComparisonFailure(String message, /*@NotNull */String expected, /*@NotNull */String actual, String expectedFilePath, String actualFilePath) {
    super(message, expected, actual);
    if (expected == null) throw new NullPointerException("'expected' must not be null");
    if (actual == null) throw new NullPointerException("'actual' must not be null");
    myMessage = message;
    myExpected = expected;
    myActual = actual;
    myFilePath = expectedFilePath;
    myActualFilePath = actualFilePath;
  }

  public String getFilePath() {
    return myFilePath;
  }

  public String getActualFilePath() {
    return myActualFilePath;
  }
  
  public String getExpected() {
    return myExpected;
  }

  public String getActual() {
    return myActual;
  }

  @Override
  public String getMessage() {
    return Assert.format(myMessage, myExpected, myActual);
  }
}
