package com.entertainment.jacklee.graficaexample.TryFragment;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

/**
 * Created by Jacklee on 16. 4. 17..
 */
public class App extends MultiDexApplication {
    private static Context appContext;


    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }


    public static Context getContext() {
        return appContext;
    }

}
