package com.example.app.fragments;

import android.os.Bundle;

import me.ilich.juggler.gui.JugglerNavigationFragment;

/**
 * Created by matek on 03.07.2017.
 */

public class NavigationFragment extends JugglerNavigationFragment {
    public static NavigationFragment create(int itemId) {
        NavigationFragment f = new NavigationFragment();
        Bundle b = new Bundle();
        addSelectedItemToBundle(b, itemId);
        f.setArguments(b);
        return f;
    }
}