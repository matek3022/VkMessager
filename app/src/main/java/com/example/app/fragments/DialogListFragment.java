package com.example.app.fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app.R;
import com.example.app.activitys.BaseActivity;
import com.example.app.fragments.states.DialogState;
import com.example.app.fragments.states.FriendListState;
import com.example.app.fragments.states.SettingState;
import com.example.app.managers.PreferencesManager;
import com.example.app.sqlite.DBHelper;
import com.example.app.transformation.CircularTransformation;
import com.example.app.vkobjects.Dialogs;
import com.example.app.vkobjects.Item;
import com.example.app.vkobjects.ItemMess;
import com.example.app.vkobjects.ServerResponse;
import com.example.app.vkobjects.User;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import me.ilich.juggler.change.Add;
import me.ilich.juggler.change.Remove;
import me.ilich.juggler.gui.JugglerFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.app.App.service;
import static com.vk.sdk.VKUIHelper.getApplicationContext;

/**
 * Created by matek on 03.07.2017.
 */

public class DialogListFragment extends JugglerFragment {

    private static final String EXTRA_FORWARD_MESS = "extra_forward_mess";

    public static DialogListFragment getInstance(String forwardMessages){
        DialogListFragment fragment = new DialogListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_FORWARD_MESS, forwardMessages);
        fragment.setArguments(args);
        return fragment;
    }

    private View addButton;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView.Adapter adapter;
    private SQLiteDatabase dataBase;
    private PreferencesManager preferencesManager;

    private String stroka = "";
    private int off = 0;
    private ArrayList<User> names;
    private ArrayList<Item> items;
    private String token;

    private String forwardMess;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataBase = DBHelper.getInstance().getWritableDatabase();
        preferencesManager = PreferencesManager.getInstance();
        token = preferencesManager.getToken();
        items = new ArrayList<>();
        names = new ArrayList<>();
        forwardMess = getArguments().getString(EXTRA_FORWARD_MESS, null);
        setHasOptionsMenu(forwardMess == null);
        addButton = view.findViewById(R.id.add_button);
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        addButton.setVisibility(forwardMess == null ? View.VISIBLE : View.GONE);
        adapter = new Adapter();
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateTo().state(Add.newActivity(new FriendListState(null), BaseActivity.class));
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (preferencesManager.getSettingOnline()) {
                    String TOKEN = preferencesManager.getToken();
                    Call<ServerResponse> call = service.setOnline(TOKEN);

                    call.enqueue(new Callback<ServerResponse>() {
                        @Override
                        public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                            off = 0;
                            refresh(off);
                        }

                        @Override
                        public void onFailure(Call<ServerResponse> call, Throwable t) {
                            refreshLayout.setRefreshing(false);
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string.LOST_INTERNET_CONNECTION), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                } else {
                    off = 0;
                    refresh(off);
                }
            }
        });
        Cursor cursor = dataBase.query(DBHelper.TABLE_DIALOGS, null, null, null, null, null, null);
        Cursor cursor1 = dataBase.query(DBHelper.TABLE_USERS, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            cursor1.moveToFirst();
            items.clear();
            names.clear();
            Gson gson = new Gson();
            int dialog = cursor.getColumnIndex(DBHelper.KEY_OBJ);
            int name = cursor1.getColumnIndex(DBHelper.KEY_OBJ);
            for (int i = 0; i < cursor.getCount(); i++) {
                items.add(gson.fromJson(cursor.getString(dialog), Item.class));
                cursor.moveToNext();
            }
            for (int i = 0; i < cursor1.getCount(); i++) {
                names.add(gson.fromJson(cursor1.getString(name), User.class));
                cursor1.moveToNext();
            }
            off=0;
            refresh(off);
        } else {
            off = 0;
            refresh(off);
        }
        cursor.close();
        cursor1.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        new UpdateDataBase(items,names).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dialog_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reload:
                off = 0;
                refresh(off);
                return true;
            case R.id.menu_setting:
                navigateTo().state(Add.newActivity(new SettingState(), BaseActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refresh(final int offset) {
        refreshLayout.setRefreshing(true);
        stroka = "";
        String TOKEN = preferencesManager.getToken();
        Call<ServerResponse<ItemMess<ArrayList<Item>>>> call = service.getDialogs(TOKEN, 20, offset);

        call.enqueue(new Callback<ServerResponse<ItemMess<ArrayList<Item>>>>() {
            @Override
            public void onResponse(Call<ServerResponse<ItemMess<ArrayList<Item>>>> call,
                                   Response<ServerResponse<ItemMess<ArrayList<Item>>>> response) {
                if (response.body().getResponse() != null) {
                    ArrayList<Item> l = response.body().getResponse().getitem();
                    if (l.size() != 0) stroka += "" + l.get(0).getMessage().getUser_id();
                    if (offset == 0) {
                        items.clear();
                    }
                    for (int i = 0; i < l.size(); i++) {
                        items.add(l.get(i));
                        if (i != 0) {
                            stroka += "," + l.get(i).getMessage().getUser_id();
                        }
                    }
                    final int UID = preferencesManager.getUserID();

                    stroka += "," + UID;
                    String TOKEN = preferencesManager.getToken();
                    Call<ServerResponse<ArrayList<User>>> call1 = service.getUser(TOKEN,
                            stroka,
                            "photo_100, online, photo_400_orig,photo_max_orig,city,country,education, universities, schools, bdate, contacts");
                    call1.enqueue(new Callback<ServerResponse<ArrayList<User>>>() {
                        @Override
                        public void onResponse(Call<ServerResponse<ArrayList<User>>> call1, Response<ServerResponse<ArrayList<User>>> response) {
                            ArrayList<User> l1 = response.body().getResponse();
                            if (l1 != null) {
                                if (offset == 0) {
                                    names.clear();
                                }
                                for (int i = 0; i < l1.size(); i++) {
                                    if (l1.get(i).getId() == UID) {
                                        preferencesManager.setUserGson(new Gson().toJson(l1.get(i)));
                                    }
                                    names.add(l1.get(i));
                                }

                                adapter.notifyDataSetChanged();
                                off = offset;
                            }
                            refreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onFailure(Call<ServerResponse<ArrayList<User>>> call1, Throwable t) {
                            refreshLayout.setRefreshing(false);
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string.LOST_INTERNET_CONNECTION), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                } else {
                    refreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<ServerResponse<ItemMess<ArrayList<Item>>>> call, Throwable t) {
                refreshLayout.setRefreshing(false);
                Toast toast = Toast.makeText(getApplicationContext(),
                        getString(R.string.LOST_INTERNET_CONNECTION), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    public static String convertMonth(int num) {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[num];
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        ImageView online;
        TextView name;
        TextView body;
        TextView time;
        TextView unreadCnt;
        RelativeLayout background;

        public ViewHolder(View itemView) {
            super(itemView);
            photo = (ImageView) itemView.findViewById(R.id.imageView4);
            online = (ImageView) itemView.findViewById(R.id.imageView5);
            name = (TextView) itemView.findViewById(R.id.textView);
            body = (TextView) itemView.findViewById(R.id.textView6);
            time = (TextView) itemView.findViewById(R.id.textView8);
            unreadCnt = (TextView) itemView.findViewById(R.id.textView9);
            background = (RelativeLayout) itemView.findViewById(R.id.background);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat day = new SimpleDateFormat("dd");
        SimpleDateFormat hour = new SimpleDateFormat("HH");
        SimpleDateFormat min = new SimpleDateFormat("mm");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm");

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(View.inflate(getActivity(), R.layout.item_list, null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Item item = items.get(position);
            final Dialogs dialog = item.getMessage();
            User user = new User();
            for (int i = 0; i < names.size(); i++) {
                if (dialog.getUser_id() == names.get(i).getId()) {
                    user = names.get(i);
                    break;
                }
            }
            final User userFinal = user;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int chatId = dialog.getChat_id();
                    int userId = dialog.getUser_id();
                    if (forwardMess != null) {
                        navigateTo().state(Remove.closeCurrentActivity());
                    }
                    navigateTo().state(Add.newActivity(new DialogState(userFinal.getFirst_name() + " " + userFinal.getLast_name(),
                            chatId == 0 ? userId : 0,
                            chatId, forwardMess), BaseActivity.class));

                }
            });
            if (dialog.getUser_id() >= 0) {
                if (dialog.getChat_id() == 0) {

                    if (user.getOnline() != 0) {
                        holder.online.setVisibility(View.VISIBLE);
                    } else {
                        holder.online.setVisibility(View.INVISIBLE);
                    }
                    if (preferencesManager.getSettingPhotoUserOn()) {
                        Picasso.with(getActivity())
                                .load(user.getPhoto_100())
                                .transform(new CircularTransformation())
                                .into(holder.photo);
                    } else {
                        Picasso.with(getActivity())
                                .load(R.drawable.soviet100)
                                .transform(new CircularTransformation())
                                .into(holder.photo);
                    }
                    holder.name.setText(user.getFirst_name() + " " + user.getLast_name());
                    if (user.getOnline() == 0) {
                        holder.online.setVisibility(View.INVISIBLE);
                    } else {
                        holder.online.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (preferencesManager.getSettingPhotoUserOn())
                        if (dialog.getPhoto_100() != null) {
                            Picasso.with(getActivity())
                                    .load(dialog.getPhoto_100())
                                    .transform(new CircularTransformation())
                                    .into(holder.photo);
                        } else {
                            Picasso.with(getActivity())
                                    .load(R.drawable.soviet100)
                                    .transform(new CircularTransformation())
                                    .into(holder.photo);
                        }
                    holder.name.setText(dialog.getTitle());
                    holder.online.setVisibility(View.INVISIBLE);
                }
            } else {
                Picasso.with(getActivity())
                        .load(R.drawable.soviet100)
                        .transform(new CircularTransformation())
                        .into(holder.photo);
                holder.name.setText(getString(R.string.COMMUNITY));
                holder.online.setVisibility(View.INVISIBLE);
            }
            if (dialog.getOut() == 0) {
                if (dialog.getAttachments().size() > 0) {
                    holder.body.setText(dialog.getAttachments().get(0).getType());
                } else {
                    if (!dialog.getFwd_messages().isEmpty()) {
                        holder.body.setText(getString(R.string.FORWARD_MESSAGES));
                    } else {
                        holder.body.setText(dialog.getBody());
                    }
                }
            } else {
                if (dialog.getAttachments().size() > 0) {
                    holder.body.setText(getString(R.string.YOU) +  dialog.getAttachments().get(0).getType() );
                } else {
                    if (!dialog.getFwd_messages().isEmpty()) {
                        holder.body.setText(getString(R.string.YOU) + getString(R.string.FORWARD_MESSAGES));
                    } else {
                        holder.body.setText(getString(R.string.YOU) + dialog.getBody());
                    }
                }
            }
            if (dialog.getAction()!=null){
                if (dialog.getAction().equals("chat_kick_user"))
                    holder.body.setText(getString(R.string.left_chat));
            }

            year.setTimeZone(TimeZone.getDefault());
            month.setTimeZone(TimeZone.getDefault());
            day.setTimeZone(TimeZone.getDefault());
            hour.setTimeZone(TimeZone.getDefault());
            min.setTimeZone(TimeZone.getDefault());
            time.setTimeZone(TimeZone.getDefault());
            Date dateCurr = new Date(System.currentTimeMillis());
            Date dateTs = new Date(dialog.getDate() * 1000L);
            String time_day = day.format(dateTs);
            String time_time = time.format(dateTs);
            String time_year = year.format(dateTs);
            if (year.format(dateTs).equals(year.format(dateCurr))) {
                if ((day.format(dateTs).equals(day.format(dateCurr))) && (month.format(dateTs).equals(month.format(dateCurr)))) {
                    holder.time.setText("" + time_time);
                } else {
                    holder.time.setText("" + time_day + " "
                            + convertMonth(Integer.parseInt(month.format(dateTs))));
                }
            } else {
                holder.time.setText("" + time_year);
            }
            if (item.getUnread() != 0) {
                holder.background.setBackgroundResource(R.color.accent);
                holder.unreadCnt.setVisibility(View.VISIBLE);
                holder.unreadCnt.setText("" + item.getUnread());
                holder.unreadCnt.setBackgroundResource(R.drawable.circleiterationdialogs);
            } else {
                holder.background.setBackgroundColor(Color.WHITE);
                holder.unreadCnt.setVisibility(View.INVISIBLE);
            }
            if (dialog.getRead_state() == 0) {
                holder.body.setBackgroundResource(R.drawable.circledialogs);
            } else {
                holder.body.setBackgroundColor(Color.WHITE);
            }
            if (position == off + 19) {

                refresh(off + 20);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    class UpdateDataBase extends AsyncTask<Void, Void, Void> {
        ArrayList<Item> items;
        ArrayList<User> names;
        public UpdateDataBase (ArrayList<Item> itemArrayList, ArrayList<User> userArrayList){
            items = new ArrayList<>();
            names = new ArrayList<>();
            items.addAll(itemArrayList);
            names.addAll(userArrayList);
        }
        @Override
        protected Void doInBackground(Void... params) {
            dataBase.beginTransaction();
            try {
                dataBase.delete(DBHelper.TABLE_DIALOGS, null, null);
                dataBase.delete(DBHelper.TABLE_USERS, null, null);
                ContentValues contentValues = new ContentValues();
                Gson gson = new Gson();

                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).getMessage().getChat_id() == 0) {
                        contentValues.put(DBHelper.KEY_ID_USER, items.get(i).getMessage().getUser_id());
                    } else {
                        contentValues.put(DBHelper.KEY_ID_USER, items.get(i).getMessage().getChat_id() + 2000000000);
                    }
                    contentValues.put(DBHelper.KEY_OBJ, gson.toJson(items.get(i)));
                    dataBase.insert(DBHelper.TABLE_DIALOGS, null, contentValues);
                }

                for (int i = 0; i < names.size(); i++) {
                    contentValues.put(DBHelper.KEY_ID_USER, names.get(i).getId());
                    contentValues.put(DBHelper.KEY_OBJ, gson.toJson(names.get(i)));
                    dataBase.insert(DBHelper.TABLE_USERS, null, contentValues);
                }
                dataBase.setTransactionSuccessful();
            }catch (Exception e){

            }finally {
                dataBase.endTransaction();
            }
            return null;
        }
    }
}
