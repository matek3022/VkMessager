package com.example.app.activitys;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.app.fragments.states.DialogListState;

import cn.nekocode.emojix.Emojix;
import me.ilich.juggler.Juggler;
import me.ilich.juggler.gui.JugglerActivity;
import me.ilich.juggler.states.State;

public class BaseActivity extends JugglerActivity{
    public static MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mediaPlayer != null) mediaPlayer = new MediaPlayer();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Juggler juggler = getJuggler(); //TODO убрать
                return juggler.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public static final String EXTRA_STATE = "extra_state";

    public static void start(Context context, State state) {
        Intent intent = new Intent(context, BaseActivity.class);
        intent.putExtra(EXTRA_STATE, state);
        context.startActivity(intent);
    }

    @Override
    protected State createState() {
        State state = (State) getIntent().getSerializableExtra(EXTRA_STATE);
        if (state == null) {
            state =  new DialogListState(null);
        }
        return state;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Emojix.wrap(newBase));
    }
}
