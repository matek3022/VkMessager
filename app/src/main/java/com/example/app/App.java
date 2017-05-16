package com.example.app;

import android.app.Application;

import com.example.app.managers.PreferencesManager;
import com.example.app.sqlite.DBHelper;
import com.example.app.utils.VKService;
import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by matek on 26.12.2016.
 */

public class App extends Application {
    public static final ArrayList <Integer> frwdMessages = new ArrayList<>();
    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.vk.com/method/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    public static final VKService service = retrofit.create(VKService.class);
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(getApplicationContext());
        DBHelper.init(getApplicationContext());
        PreferencesManager.init(getApplicationContext());
    }
}
