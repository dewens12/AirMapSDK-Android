package com.airmap.airmapsdktest;

import androidx.multidex.MultiDexApplication;

import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.util.AirMapConfig;
import com.mapbox.mapboxsdk.Mapbox;

import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class MyApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        AirMap.init(this);
        Timber.plant(new DebugTree());

        Mapbox.getInstance(this, AirMapConfig.getMapboxApiKey());
    }
}
