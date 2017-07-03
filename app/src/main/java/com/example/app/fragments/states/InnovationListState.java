package com.example.app.fragments.states;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.example.app.R;

import me.ilich.juggler.gui.JugglerFragment;
import me.ilich.juggler.states.ContentToolbarNavigationState;
import me.ilich.juggler.states.VoidParams;

/**
 * Created by Sergey Elizarov (sergey.elizarov@altarix.ru)
 * on 28.04.17 12:26.
 */

public class InnovationListState extends ContentToolbarNavigationState<VoidParams> {

    public InnovationListState() {
        super(VoidParams.instance());
    }

    @Override
    public String getTitle(Context context, VoidParams params) {
        return "title";
    }

    @Override
    public Drawable getUpNavigationIcon(Context context, VoidParams params) {
        return context.getResources().getDrawable(R.drawable.circle);
    }

    @Override
    protected JugglerFragment onConvertContent(VoidParams params, @Nullable JugglerFragment fragment) {
//        return InnovationListFragment.newInstance();
            return null;
    }

    @Override
    protected JugglerFragment onConvertToolbar(VoidParams params, @Nullable JugglerFragment fragment) {
        return null;
//        return ToolbarShadowFragment.createNavigation();
    }


    @Override
    protected JugglerFragment onConvertNavigation(VoidParams params, @Nullable JugglerFragment fragment) {
        return null;
//        return NavigationFragment.create(R.id.menu_innovations);
    }
}
