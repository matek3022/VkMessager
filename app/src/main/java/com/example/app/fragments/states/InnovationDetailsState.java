package com.example.app.fragments.states;

/**
 * Created by Sergey Elizarov (sergey.elizarov@altarix.ru)
 * on 31.05.17 10:32.
 */

//public class InnovationDetailsState extends ContentBelowToolbarState<InnovationDetailsState.Params> {
//
//    public static final String TAG = "innovation_details";
//
//    public InnovationDetailsState(int id) {
//        super(new InnovationDetailsState.Params(id));
//    }
//
//    @Nullable
//    @Override
//    public String getTitle(Context context, InnovationDetailsState.Params params) {
//        return context.getString(R.string.title_innovation_details);
//    }
//
//    @Override
//    public Drawable getUpNavigationIcon(Context context, InnovationDetailsState.Params params) {
//        return context.getResources().getDrawable(R.drawable.ic_nav_arrow);
//    }
//
//    @Nullable
//    @Override
//    public String getTag() {
//        return TAG;
//    }
//
//    @Override
//    protected JugglerFragment onConvertContent(InnovationDetailsState.Params params, @Nullable JugglerFragment fragment) {
//        return InnovationDetailsFragment.newInstance(params.id);
//    }
//
//    @Override
//    protected JugglerFragment onConvertToolbar(InnovationDetailsState.Params params, @Nullable JugglerFragment fragment) {
//        return ToolbarShadowFragment.createNavigation();
//    }
//
//    public static class Params extends ContentBelowToolbarState.Params {
//        private final int id;
//
//        public Params(int id) {
//            this.id = id;
//        }
//    }
//}
