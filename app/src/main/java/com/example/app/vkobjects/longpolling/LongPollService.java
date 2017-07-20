package com.example.app.vkobjects.longpolling;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.app.managers.PreferencesManager;
import com.example.app.vkobjects.ServerResponse;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.app.App.service;

/**
 * Created by matek on 20.07.2017.
 */

public class LongPollService extends Service {
    private static final long INTERNET_DELAY = 5000L;

    private PreferencesManager preferencesManager;
    private String token;
    private GetLpSrvr getLpSrvr;

    private boolean isRunning = false;
    private Handler handler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    initPrefs();
                    updateLongPoll();
                }
            }).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initPrefs() {
        preferencesManager = PreferencesManager.getInstance();
        token = preferencesManager.getToken();
    }

    private void startRequest() {
        service.connect("https://" + getLpSrvr.getServer(), getLpSrvr.getKey(), getLpSrvr.getTs(),"a_check", 25, 2).enqueue(new Callback<LongPollResponse>() {
            @Override
            public void onResponse(Call<LongPollResponse> call, Response<LongPollResponse> response) {
                Log.i("ResponseLongPoll", response.toString());
                ArrayList<LongPollEvent> longPollEvents = response.body().updates;
                sendBroadcasts(longPollEvents);
                getLpSrvr.setTs(response.body().getTs());
                startRequest();
            }

            @Override
            public void onFailure(Call<LongPollResponse> call, Throwable t) {
                if (t.getMessage().contains("Unable")) {
                    Toast.makeText(getApplicationContext(), "Произошла ошибка! Мы уже перезапускаем сервис", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            restartService();
                        }
                    }, INTERNET_DELAY);
                } else {
                    updateLongPoll();
                }
            }
        });
    }

    private void updateLongPoll() {
        service.getLongPollServer(token).enqueue(new Callback<ServerResponse<GetLpSrvr>>() {
            @Override
            public void onResponse(Call<ServerResponse<GetLpSrvr>> call, Response<ServerResponse<GetLpSrvr>> response) {
                getLpSrvr = response.body().getResponse();
                startRequest();
            }

            @Override
            public void onFailure(Call<ServerResponse<GetLpSrvr>> call, Throwable t) {
                if (t.getMessage().contains("Unable")) {
                    Toast.makeText(getApplicationContext(), "Произошла ошибка! Мы уже перезапускаем сервис", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            restartService();
                        }
                    }, INTERNET_DELAY);
                } else {
                    updateLongPoll();
                }
            }
        });
    }

    private void restartService() {
        isRunning = false;
        startService(new Intent(this, LongPollService.class));
    }

    private void sendBroadcasts(ArrayList<LongPollEvent> longPollEvents) {
        for (int i = 0; i < longPollEvents.size(); i++) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            longPollEvents.get(i).init();
            Intent intent = null;
            switch (longPollEvents.get(i).type){
                case LongPollEvent.NEW_MESSAGE_EVENT:
                    intent = new Intent(LongPollEvent.NEW_MESSAGE_INTENT);
                    break;
                case LongPollEvent.READ_IN_EVENT:
                    intent = new Intent(LongPollEvent.READ_IN_INTENT);
                    break;
                case LongPollEvent.READ_OUT_EVENT:
                    intent = new Intent(LongPollEvent.READ_OUT_INTENT);
                    break;
                case LongPollEvent.ONLINE_EVENT:
                    intent = new Intent(LongPollEvent.ONLINE_INTENT);
                    break;
                case LongPollEvent.OFFLINE_EVENT:
                    intent = new Intent(LongPollEvent.OFFLINE_INTENT);
                    break;
                case LongPollEvent.TYPING_IN_USER_EVENT:
                    intent = new Intent(LongPollEvent.TYPING_IN_USER_INTENT);
                    break;
                case LongPollEvent.TYPING_IN_CHAT_EVENT:
                    intent = new Intent(LongPollEvent.TYPING_IN_CHAT_INTENT);
                    break;
                case LongPollEvent.NEW_COUNT_EVENT:
                    intent = new Intent(LongPollEvent.NEW_COUNT_INTENT);
                    break;
            }
            if (intent != null) {
                intent.putExtra(LongPollEvent.INTENT_EXTRA_SERIALIZABLE, longPollEvents.get(i));
                localBroadcastManager.sendBroadcast(intent);
            }
        }
    }
}
