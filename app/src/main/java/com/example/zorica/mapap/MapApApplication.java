package com.example.zorica.mapap;

import android.app.Application;
import com.example.zorica.mapap.utils.FontsOverride;

/**
 * Created by Zorica on 14-Nov-16.
 */

public class MapApApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/opensans-regular.ttf");

    }
}
