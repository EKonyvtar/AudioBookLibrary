package com.murati.oszk.audiobook.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * Created by akosmurati on 18/11/17.
 * https://stackoverflow.com/questions/4985805/set-locale-programmatically
 */


public class LanguageHelper {
  public static final void setDefaultAppLocale(Activity activity) {
    setAppLocale(activity,"hu", "HU");
  }

  public static final void setAppLocale(Activity activity, String language, String country) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      Resources resources = activity.getResources();
      Configuration configuration = resources.getConfiguration();
      configuration.setLocale(new Locale(language,country));
      activity.getApplicationContext().createConfigurationContext(configuration);
    } else {
      Locale locale = new Locale(language,country);
      Locale.setDefault(locale);
      Configuration config = activity.getResources().getConfiguration();
      config.locale = locale;
      activity.getResources().updateConfiguration(config,
        activity.getResources().getDisplayMetrics());
    }
  }
}
