package com.example.app;

import android.app.Application;

import com.example.app.managers.PreferencesManager;
import com.example.app.sqlite.DBHelper;
import com.example.app.utils.VKService;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.vk.sdk.VKSdk;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by matek on 26.12.2016.
 */

public class App extends Application {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(50, TimeUnit.SECONDS)
            .readTimeout(50,TimeUnit.SECONDS).build();
    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.vk.com/method/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build();
    public static final VKService service = retrofit.create(VKService.class);

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(getApplicationContext());
        Fresco.initialize(getApplicationContext());
        DBHelper.init(getApplicationContext());
        PreferencesManager.init(getApplicationContext());
    }
}
