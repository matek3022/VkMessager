package com.example.app.activitys;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.app.R;
import com.example.app.fragments.states.InnovationListState;

import me.ilich.juggler.Juggler;
import me.ilich.juggler.change.Add;
import me.ilich.juggler.change.Remove;
import me.ilich.juggler.gui.JugglerActivity;

public class BaseActivity extends JugglerActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
    }

    public void relogin() {
        navigateTo().state(Remove.closeAllActivities(),
                Add.newActivity(new InnovationListState(), BaseActivity.class));
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
}
