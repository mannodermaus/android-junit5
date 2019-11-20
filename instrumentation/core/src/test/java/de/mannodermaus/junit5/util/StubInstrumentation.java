package de.mannodermaus.junit5.util;

import android.app.Instrumentation;
import android.content.Context;

import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

class StubInstrumentation extends Instrumentation {

  private final Context targetContext = mock(Context.class);

  StubInstrumentation() {
    // Setup Context mock
    String packageName = BuildConfig.class.getName();
    packageName = packageName.substring(0, packageName.lastIndexOf('.'));
    Mockito.when(targetContext.getPackageName()).thenReturn(packageName);
  }

  @Override
  public Context getTargetContext() {
    return targetContext;
  }
}
