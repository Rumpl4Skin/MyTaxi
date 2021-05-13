package com.example.mytaxi;

import androidx.multidex.MultiDexApplication;

import com.teliver.sdk.core.Teliver;


public class Application extends MultiDexApplication {

    public static final String TRACKING_ID = "tracking_id";


    @Override
    public void onCreate() {
        super.onCreate();
        Teliver.init(this, "ba51fec4f189fda39efdb12af7262952");
    }
}
