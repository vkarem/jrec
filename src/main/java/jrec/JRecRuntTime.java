package jrec;

import lombok.Getter;
import lombok.Setter;

public class JRecRuntTime implements TestListener {
  public static final String DEFAULT_CHARSET = "UTF-8";

  @Getter
  private final static JRecRuntTime runtTime = new JRecRuntTime();

  private JRecRuntTime(){}

  public static String getCurrentTest(){
    return runtTime.getCurrentTest();
  }

  public static void setCurrentTest(String testName){
    runtTime.setCurrentTest(testName);
  }

  public static void registerRecorder(Recorder recorder){
    runtTime.setRecorder(recorder);
  }

  @Getter
  private String currentTest;

  @Setter
  private Recorder recorder;

  @Override
  public void beforeTestMethod(String testName) {
    currentTest = testName;
  }

  @Override
  public void afterTestMethod(String testName) {
    currentTest = null;
  }

  @Override
  public void beforeTestClass() {
    //unzip here
  }

  @Override
  public void afterTestClass() {
    //Zip here
  }
}
