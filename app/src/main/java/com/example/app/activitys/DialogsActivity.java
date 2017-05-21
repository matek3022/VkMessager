package com.example.app.activitys;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app.R;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.app.App.service;

public class DialogsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static MediaPlayer mediaPlayer;
    SwipeRefreshLayout refreshLayout;
    Adapter adapter;
    String stroka = "";
    int off = 0;
    ArrayList<User> names;
    ArrayList<Item> items;
    private RecyclerView recyclerView;
    SQLiteDatabase dataBase;
    PreferencesManager preferencesManager;
    private static final String EXTRA_FORWARD_MESSAGE="frwd";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        dataBase = DBHelper.getInstance().getWritableDatabase();
        preferencesManager = PreferencesManager.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialogs);

        items = new ArrayList<>();
        names = new ArrayList<>();

        recyclerView = (RecyclerView) findViewById(R.id.list);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        adapter = new Adapter();
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final String uidgson = preferencesManager.getUserGson();
        if (uidgson != "") {
            final User iuser = new Gson().fromJson(uidgson, User.class);
            Picasso.with(DialogsActivity.this)
                    .load(iuser.getPhoto_100())
                    .transform(new CircularTransformation())
                    .into((ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView20));
            ((ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView20)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DialogsActivity.this, UserActivity.class);
                    intent.putExtra("userID", iuser.getId());
                    intent.putExtra("userJson", uidgson);
                    startActivity(intent);
                }
            });
            if (iuser.getOnline() == 1) {
                ((ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView21)).setVisibility(View.VISIBLE);
            } else {
                ((ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView21)).setVisibility(View.INVISIBLE);
            }
            ((TextView) navigationView.getHeaderView(0).findViewById(R.id.textView20)).setText(iuser.getFirst_name() + " " + iuser.getLast_name());
            if (iuser.getCity() != (null)) {
                ((TextView) navigationView.getHeaderView(0).findViewById(R.id.textView21)).setText(iuser.getCity().getTitle());
            } else {
                ((TextView) navigationView.getHeaderView(0).findViewById(R.id.textView21)).setText("");
            }
        }

        setTitle(getString(R.string.dialogs));
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
    protected void onStop() {
        new UpdateDataBase(items,names).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        super.onStop();
    }

    static Intent getIntent (Context context, boolean frwdMessDetector, boolean clearStack){
        Intent intent = new Intent(context, DialogsActivity.class);
        if (clearStack){
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(EXTRA_FORWARD_MESSAGE,frwdMessDetector);
        return intent;
    }

    class UpdateDataBase extends AsyncTask<Void, Void, Void>{
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


    public void refresh(final int offset) {
        refreshLayout.setRefreshing(true);
        stroka = "";
        String TOKEN = preferencesManager.getToken();
        Call<ServerResponse<ItemMess<ArrayList<Item>>>> call = service.getDialogs(TOKEN, 20, offset);

        call.enqueue(new Callback<ServerResponse<ItemMess<ArrayList<Item>>>>() {
            @Override
            public void onResponse(Call<ServerResponse<ItemMess<ArrayList<Item>>>> call,
                                   Response<ServerResponse<ItemMess<ArrayList<Item>>>> response) {
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

//    @Override
//    protected void attachBaseContext(Context newBase) {
//        super.attachBaseContext(Emojix.wrap(newBase));
//    }

    public static String convertMonth(int num) {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[num];
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dialog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.titleee:
                refresh(0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view Item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_dialogs) {
            startActivity(DialogsActivity.getIntent(DialogsActivity.this, false, true));
            DialogsActivity.this.finish();

        } else if (id == R.id.nav_friends) {
            startActivity(FriendsActivity.getIntent(DialogsActivity.this, 0, true));
            DialogsActivity.this.finish();
        } else if (id == R.id.nav_settings) {
            startActivity(SettingActivity.getIntent(DialogsActivity.this));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            return new ViewHolder(View.inflate(DialogsActivity.this, R.layout.item_list, null));
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
                public void onClick(View v) {
                    if (getIntent().getBooleanExtra(EXTRA_FORWARD_MESSAGE, false)) {
                        DialogsActivity.this.finish();
                    }
                    startActivity(DialogMessageActivity.getIntent(DialogsActivity.this, dialog.getUser_id(), dialog.getChat_id(), dialog.getTitle(), userFinal.getFirst_name() + " " + userFinal.getLast_name(), getIntent().getBooleanExtra("frwd", false)));
                }
            });
            holder.photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialog.getChat_id() == 0) {
                        startActivity(UserActivity.getIntent(DialogsActivity.this, dialog.getUser_id(), new Gson().toJson(userFinal)));
                    }
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
                        Picasso.with(DialogsActivity.this)
                                .load(user.getPhoto_100())
                                .transform(new CircularTransformation())
                                .into(holder.photo);
                    } else {
                        Picasso.with(DialogsActivity.this)
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
                            Picasso.with(DialogsActivity.this)
                                    .load(dialog.getPhoto_100())
                                    .transform(new CircularTransformation())
                                    .into(holder.photo);
                        } else {
                            Picasso.with(DialogsActivity.this)
                                    .load(R.drawable.soviet100)
                                    .transform(new CircularTransformation())
                                    .into(holder.photo);
                        }
                    holder.name.setText(dialog.getTitle());
                    holder.online.setVisibility(View.INVISIBLE);
                }
            } else {
                Picasso.with(DialogsActivity.this)
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
}
