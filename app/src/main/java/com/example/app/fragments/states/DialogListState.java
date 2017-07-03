package com.example.app.fragments.states;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.example.app.R;
import com.example.app.fragments.DialogListFragment;
import com.example.app.fragments.NavigationFragment;
import com.example.app.fragments.ToolbarFragment;

import me.ilich.juggler.gui.JugglerFragment;
import me.ilich.juggler.states.ContentToolbarNavigationState;
import me.ilich.juggler.states.VoidParams;

/**
 * Created by matek on 03.07.2017.
 */

public class DialogListState extends ContentToolbarNavigationState<VoidParams> {

    public DialogListState() {
        super(VoidParams.instance());
    }

    @Override
    public String getTitle(Context context, VoidParams params) {
        return "Диалоги";
    }

    @Override
    public Drawable getUpNavigationIcon(Context context, VoidParams params) {
        return context.getResources().getDrawable(R.drawable.ic_navigate_burger);
    }

    @Override
    protected JugglerFragment onConvertContent(VoidParams params, @Nullable JugglerFragment fragment) {
        return new DialogListFragment();
    }

    @Override
    protected JugglerFragment onConvertToolbar(VoidParams params, @Nullable JugglerFragment fragment) {
        return ToolbarFragment.createNavigation();
    }

    @Override
    protected JugglerFragment onConvertNavigation(VoidParams params, @Nullable JugglerFragment fragment) {
        return NavigationFragment.create(0);
    }
}
