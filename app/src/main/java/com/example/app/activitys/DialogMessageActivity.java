package com.example.app.activitys;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app.R;
import com.example.app.managers.PreferencesManager;
import com.example.app.sqlite.DBHelper;
import com.example.app.transformation.CircularTransformation;
import com.example.app.utils.CryptUtils;
import com.example.app.utils.Util;
import com.example.app.vkobjects.Attachment;
import com.example.app.vkobjects.Dialogs;
import com.example.app.vkobjects.ItemMess;
import com.example.app.vkobjects.ServerResponse;
import com.example.app.vkobjects.User;
import com.example.app.vkobjects.VideoInformation;
import com.google.gson.Gson;
import com.luseen.autolinklibrary.AutoLinkMode;
import com.luseen.autolinklibrary.AutoLinkOnClickListener;
import com.luseen.autolinklibrary.AutoLinkTextView;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;
import com.squareup.picasso.Picasso;
import com.stfalcon.frescoimageviewer.ImageViewer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import cn.nekocode.emojix.Emojix;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.app.App.frwdMessages;
import static com.example.app.App.service;

public class DialogMessageActivity extends AppCompatActivity {

    private int user_id;
    private int chat_id;
    private String title;
    private boolean frwd;
    Adapter adapter;
    Button sendButton;
    private RecyclerView recyclerView;
    SwipyRefreshLayout refreshLayout;
    int off;
    ArrayList<Dialogs> items;
    ArrayList<User> names;
    ArrayList<Integer> namesIds;
    SQLiteDatabase dataBase;
    PreferencesManager preferencesManager;
    EmojiconEditText mess;
    Button forwardButton;
    private static final String EXTRA_USER_ID="userID";
    private static final String EXTRA_TITLE="Title";
    private static final String EXTRA_USER_NAME="userName";
    private static final String EXTRA_CHAT_ID="ChatID";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_message);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        frwd=false;
        dataBase = DBHelper.getInstance().getWritableDatabase();
        preferencesManager = PreferencesManager.getInstance();
        user_id = getIntent().getIntExtra(EXTRA_USER_ID, 0);
        chat_id = getIntent().getIntExtra(EXTRA_CHAT_ID, 0);
        items = new ArrayList<>();
        names = new ArrayList<>();
        namesIds = new ArrayList<>();
        adapter = new Adapter();
        recyclerView = (RecyclerView) findViewById(R.id.list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        mess = (EmojiconEditText) findViewById(R.id.editText);
        refreshLayout = (SwipyRefreshLayout) findViewById(R.id.refresh);
        sendButton = (Button) findViewById(R.id.button);
        forwardButton = (Button) findViewById(R.id.fab);
        ImageView imageEmoji = (ImageView)findViewById(R.id.emoji_button);
        imageEmoji.setImageResource(R.drawable.smiley);

        EmojIconActions emojIconActions = new EmojIconActions(this, findViewById(R.id.activity_main3), mess,imageEmoji);
        emojIconActions.ShowEmojIcon();

        if (chat_id != 0) {
            user_id = 2000000000 + chat_id;
            title = getIntent().getStringExtra(EXTRA_TITLE);
        } else {
            if (user_id < 0) {
                title = getString(R.string.COMMUNITY);
            } else {
                title = getIntent().getStringExtra(EXTRA_USER_NAME);
            }
        }

        if (frwdMessages.size()>0) mess.setHint(" "+ frwdMessages.size()+ " " + getString(R.string.FORWARD_MESSAGES));

        llm.setOrientation(LinearLayoutManager.VERTICAL);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);

        setTitle(title);

        refreshLayout.setColorSchemeResources(R.color.accent);

        refreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                if (direction == SwipyRefreshLayoutDirection.BOTTOM) {
                    if (off != 0) {
                        off -= 100;
                    }
                    refresh(off);
                } else {
                    off += 100;
                    refresh(off);
                }
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!frwd) {
                    refreshLayout.setRefreshing(true);
                    if ((!mess.getText().toString().equals(""))||(frwdMessages.size()>0)) {
                        String message = mess.getText().toString();
                        mess.setText("");
                        int kek = user_id;
                        if (chat_id != 0) {
                            kek = 0;
                        }

                        String strIdMess="";
                        for (int i=0;i<frwdMessages.size();i++){
                            strIdMess+=","+frwdMessages.get(i);
                        }

                        String TOKEN = preferencesManager.getToken();
                        Call<ServerResponse> call = service.sendMessage(TOKEN, kek, message, chat_id, 2000000000 + chat_id,strIdMess);

                        call.enqueue(new Callback<ServerResponse>() {
                            @Override
                            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                                Log.wtf("motya", response.raw().toString());
                                frwdMessages.clear();
                                mess.setHint (getString(R.string.WRITE_MESSAGE));
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
                        refreshLayout.setRefreshing(false);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.VOID_MESSAGE), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }else {
                    refreshLayout.setRefreshing(false);
                }
            }
        });

        sendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                refreshLayout.setRefreshing(true);
                if ((!mess.getText().toString().equals(""))||(frwdMessages.size()>0)) {
                    String message = mess.getText().toString();
                    message = CryptUtils.cryptWritibleString(message);
                    mess.setText("");
                    int kek = user_id;
                    if (chat_id != 0) {
                        kek = 0;
                    }

                    String strIdMess="";
                    for (int i=0;i<frwdMessages.size();i++){
                        strIdMess+=","+frwdMessages.get(i);
                    }

                    String TOKEN = preferencesManager.getToken();
                    Call<ServerResponse> call = service.sendMessage(TOKEN, kek, message, chat_id, 2000000000 + chat_id,strIdMess);

                    call.enqueue(new Callback<ServerResponse>() {
                        @Override
                        public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                            Log.wtf("motya", response.raw().toString());
                            frwdMessages.clear();
                            mess.setHint (getString(R.string.WRITE_MESSAGE));
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
                    refreshLayout.setRefreshing(false);
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getString(R.string.VOID_MESSAGE), Toast.LENGTH_SHORT);
                    toast.show();
                }
                return true;
            }
        });

        Cursor cursor = dataBase.query(DBHelper.TABLE_MESSAGES,null,DBHelper.KEY_ID_DIALOG + " = ?",new String[]{user_id+""},null,null,DBHelper.KEY_TIME_MESSAGES);
        Cursor cursor1 = dataBase.query(DBHelper.TABLE_USERS_IN_MESSAGES, null, DBHelper.KEY_ID_DIALOG + " = ?", new String[]{user_id+""}, null, null, DBHelper.KEY_ID);
        Log.i("dataBase",cursor.getCount() + " "+ cursor1.getCount());
        if (cursor.moveToFirst()) {
            Log.i("dataBase",cursor.getCount() + " "+ cursor1.getCount());
            cursor1.moveToFirst();
            items.clear();
            names.clear();
            Gson gson = new Gson();
            int dialog = cursor.getColumnIndex(DBHelper.KEY_OBJ);
            int name = cursor1.getColumnIndex(DBHelper.KEY_OBJ);
            for (int i = 0; i < cursor.getCount(); i++) {
                items.add(gson.fromJson(cursor.getString(dialog), Dialogs.class));
                cursor.moveToNext();
            }
            for (int i = 0; i < cursor1.getCount(); i++) {
                names.add(gson.fromJson(cursor1.getString(name), User.class));
                Log.i("motyaChat", "" + names.get(i).getFirst_name());
                cursor1.moveToNext();
            }
            adapter.reserv.addAll(items);
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
    protected void onResume() {
        if (frwdMessages.size()>0){
            mess.setHint(" "+ frwdMessages.size()+ " " + getString(R.string.FORWARD_MESSAGES));
        }else{
            mess.setHint(getString(R.string.WRITE_MESSAGE));
        }
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    protected void onStop() {
        new UpdateDataBase(user_id,items,names).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        super.onStop();
    }


    static Intent getIntent (Context context, int userId, int chatId, String title, String userName, boolean frwdMessDetector){
        Intent intent = new Intent(context, DialogMessageActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        if (!frwdMessDetector){
            frwdMessages.clear();
        }
        return intent;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Emojix.wrap(newBase));
    }

    class UpdateDataBase extends AsyncTask<Void, Void, Void> {
        ArrayList<Dialogs> items;
        ArrayList<User> names;
        int user_id;
        public UpdateDataBase (int id,ArrayList<Dialogs> itemArrayList, ArrayList<User> userArrayList){
            items = new ArrayList<>();
            names = new ArrayList<>();
            items.addAll(itemArrayList);
            names.addAll(userArrayList);
            user_id=id;
        }
        @Override
        protected Void doInBackground(Void... params) {
            dataBase.beginTransaction();
            try {
                int howmuch=0;
                howmuch=dataBase.delete(DBHelper.TABLE_MESSAGES, DBHelper.KEY_ID_DIALOG +" = "+user_id, null);
                Log.i("howMuch",howmuch+"");
                howmuch=dataBase.delete(DBHelper.TABLE_USERS_IN_MESSAGES, DBHelper.KEY_ID_DIALOG +" = "+user_id, null);
                Log.i("howMuch",howmuch+"");
                ContentValues contentValues = new ContentValues();
                Gson gson = new Gson();

                for (int i = 0; i < items.size(); i++) {
                    contentValues.put(DBHelper.KEY_ID_DIALOG, user_id);
                    contentValues.put(DBHelper.KEY_TIME_MESSAGES,items.get(i).getDate());
                    contentValues.put(DBHelper.KEY_OBJ, gson.toJson(items.get(i)));
                    dataBase.insert(DBHelper.TABLE_MESSAGES, null, contentValues);
                }
                ContentValues contentValues1 = new ContentValues();
                for (int i = 0; i < names.size(); i++) {
                    contentValues1.put(DBHelper.KEY_ID_DIALOG, user_id);
                    contentValues1.put(DBHelper.KEY_OBJ, gson.toJson(names.get(i)));
                    long num=0;
                    num=dataBase.insert(DBHelper.TABLE_USERS_IN_MESSAGES, null, contentValues1);
                    Log.i("namesChat", "" + names.get(i).getFirst_name()+" "+num);
                }
                dataBase.setTransactionSuccessful();
            }catch (Exception e){

            }finally {
                dataBase.endTransaction();
            }
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.fwd_mess.size() > 1) {
            adapter.fwd_mess.remove(adapter.fwd_mess.size() - 1);
            items = adapter.fwd_mess.get(adapter.fwd_mess.size() - 1);
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(adapter.pos.get(adapter.pos.size() - 1));
            adapter.pos.remove(adapter.pos.size() - 1);
        } else {
            if (adapter.fwd_mess.size() == 1) {
                items.clear();
                items.addAll(adapter.reserv);
                frwd=false;
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(adapter.pos.get(adapter.pos.size() - 1));
                adapter.fwd_mess.clear();
                adapter.pos.remove(adapter.pos.size() - 1);
            } else {
                super.onBackPressed();
            }
        }
    }


    public void nameRec(Dialogs contain_mess) {
        boolean chek=false;
        for (int i = 0; i < namesIds.size();i++){
            if (namesIds.get(i)==contain_mess.getUser_id()){
                chek=true;
            }
        }
        if (!chek) {
            namesIds.add(contain_mess.getUser_id());
        }
        for (int i = 0; i < contain_mess.getFwd_messages().size(); i++) {
            nameRec(contain_mess.getFwd_messages().get(i));
        }
    }

    public void refresh(final int offset) {
        if (!frwd) {
            refreshLayout.setRefreshing(true);

            String TOKEN = preferencesManager.getToken();
            Call<ServerResponse<ItemMess<ArrayList<Dialogs>>>> call = service.getHistory(TOKEN, 100, offset, user_id);

            call.enqueue(new Callback<ServerResponse<ItemMess<ArrayList<Dialogs>>>>() {
                @Override
                public void onResponse(Call<ServerResponse<ItemMess<ArrayList<Dialogs>>>> call, Response<ServerResponse<ItemMess<ArrayList<Dialogs>>>> response) {
                    Log.wtf("motya", response.raw().toString());
                    ArrayList<Dialogs> l = response.body().getResponse().getitem();
                    String people_id = "" + l.get(0).getUser_id();
                    namesIds.clear();
                    if (offset == 0) {
                        items.clear();
                        for (int i = 0; i<l.size();i++){
                            nameRec(l.get(i));
                            items.add(0,l.get(i));
                        }
                    } else {
                        for (int i = 0; i<l.size();i++){
                            nameRec(l.get(i));
                            items.add(0,l.get(i));
                        }
                    }
                    for (int i = 0; i < namesIds.size(); i++){
                        people_id+=  "," + namesIds.get(i);
                    }
                    people_id += ", " + preferencesManager.getUserID();
                    Log.i ("chek",people_id);
                    refreshLayout.setRefreshing(false);

                    String TOKEN = preferencesManager.getToken();
                    Call<ServerResponse<ArrayList<User>>> call1 = service.getUser(TOKEN, people_id, "photo_100,photo_400_orig,photo_max_orig, online,city,country,education, universities, schools,bdate,contacts");

                    call1.enqueue(new Callback<ServerResponse<ArrayList<User>>>() {
                        @Override
                        public void onResponse(Call<ServerResponse<ArrayList<User>>> call1, Response<ServerResponse<ArrayList<User>>> response) {
                            Log.wtf("motya", response.raw().toString());
                            ArrayList<User> l = response.body().getResponse();
                            if (offset == 0) {
                                names.clear();
                            }
                            for (int i = 0; i < l.size(); i++) {
                                names.add(l.get(i));
                            }
                            refreshLayout.setRefreshing(false);
                            recyclerView.scrollToPosition(items.size() - offset);
                            adapter.fwd_mess.clear();
                            adapter.reserv.clear();
                            adapter.reserv.addAll(items);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(Call<ServerResponse<ArrayList<User>>> call1, Throwable t) {
                            Log.wtf("chek", t.getLocalizedMessage());
                            refreshLayout.setRefreshing(false);
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string.LOST_INTERNET_CONNECTION), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                }

                @Override
                public void onFailure(Call<ServerResponse<ItemMess<ArrayList<Dialogs>>>> call, Throwable t) {
                    refreshLayout.setRefreshing(false);
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getString(R.string.LOST_INTERNET_CONNECTION), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }else {
            refreshLayout.setRefreshing(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.titleee:
                off = 0;
                refresh(off);
                return true;
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static String convertMonth(int num) {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[num];
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        ImageView online;
        AutoLinkTextView body;
        TextView time;
        RelativeLayout background;
        LinearLayout line;
        RelativeLayout foo;

        public ViewHolder(View itemView) {
            super(itemView);
            photo = (ImageView) itemView.findViewById(R.id.imageView);
            online = (ImageView) itemView.findViewById(R.id.imageView6);
            body = (AutoLinkTextView) itemView.findViewById(R.id.textView2);
            time = (TextView) itemView.findViewById(R.id.textView);
            background = (RelativeLayout) itemView.findViewById(R.id.relativeLayout);
            line = (LinearLayout) itemView.findViewById(R.id.line);
            foo = (RelativeLayout) itemView.findViewById(R.id.foo);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat day = new SimpleDateFormat("dd");
        SimpleDateFormat hour = new SimpleDateFormat("HH");
        SimpleDateFormat min = new SimpleDateFormat("mm");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm");
        ArrayList<ArrayList<Dialogs>> fwd_mess;
        ArrayList<Dialogs> reserv;
        ArrayList<Integer> pos;

        public Adapter() {
            fwd_mess = new ArrayList<>();
            reserv = new ArrayList<>();
            pos = new ArrayList<>();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getOut();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ViewHolder(View.inflate(DialogMessageActivity.this, R.layout.messin, null));
            } else {
                return new ViewHolder(View.inflate(DialogMessageActivity.this, R.layout.messout, null));
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Dialogs dialog = items.get(position);
            final int finalPos = position;
            User user = new User();
            for (int i = 0; i < names.size(); i++) {
                if (dialog.getOut()==0) {
                    if (dialog.getUser_id() == names.get(i).getId()) {
                        user = names.get(i);
                        break;
                    }
                }else {
                    if (dialog.getFrom_id() == names.get(i).getId()) {
                        user = names.get(i);
                        break;
                    }
                }
            }

            if (dialog.getRead_state() == 0) {
                holder.foo.setBackgroundColor(ContextCompat.getColor(DialogMessageActivity.this, R.color.accent));
            }else {
                holder.foo.setBackgroundColor(Color.WHITE);
            }
            for (int i=0;i<frwdMessages.size();i++){
                if (dialog.getId()==frwdMessages.get(i)){
                    holder.foo.setBackgroundColor(ContextCompat.getColor(DialogMessageActivity.this, R.color.primary_dark));
                }
            }
            holder.line.removeAllViews();
            if (user.getOnline() != 0) {
                holder.online.setVisibility(View.VISIBLE);
            } else {
                holder.online.setVisibility(View.INVISIBLE);
            }
            if (preferencesManager.getSettingPhotoUserOn()) {
                Picasso.with(DialogMessageActivity.this)
                        .load(user.getPhoto_100())
                        .transform(new CircularTransformation())
                        .into(holder.photo);
            } else {
                Picasso.with(DialogMessageActivity.this)
                        .load(R.drawable.soviet100)
                        .transform(new CircularTransformation())
                        .into(holder.photo);
            }
            final User userFinal = user;
            final ViewHolder viewHolder = holder;

            if (frwdMessages.size()==0) forwardButton.setVisibility(View.INVISIBLE);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean chek = false;
                    for (int i=0;i<frwdMessages.size();i++){
                        if (frwdMessages.get(i)==dialog.getId()){
                            frwdMessages.remove(i);
                            if (dialog.getRead_state()==1) {
                                viewHolder.foo.setBackgroundColor(Color.WHITE);
                            }else {
                                viewHolder.foo.setBackgroundColor(ContextCompat.getColor(DialogMessageActivity.this, R.color.accent));
                            }
                            chek=true;
                            break;
                        }
                    }

                    if (!chek){
                        frwdMessages.add (dialog.getId());
                        viewHolder.foo.setBackgroundColor(ContextCompat.getColor(DialogMessageActivity.this, R.color.primary_dark));
                    }

                    if (frwdMessages.size()>0){
                        mess.setHint(" "+ frwdMessages.size()+ " " + getString(R.string.FORWARD_MESSAGES));
                    }else{
                        mess.setHint(getString(R.string.WRITE_MESSAGE));
                    }

                    if (frwdMessages.size()>0){
                        forwardButton.setVisibility(View.VISIBLE);
                        forwardButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(DialogsActivity.getIntent(DialogMessageActivity.this,true,false));
                            }
                        });
                    }else {
                        forwardButton.setVisibility(View.INVISIBLE);
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    holder.body.setText(CryptUtils.decryptWritibleString(holder.body.getText().toString()));
                    return true;
                }
            });
            holder.photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        startActivity(UserActivity.getIntent(DialogMessageActivity.this,userFinal.getId(),new Gson().toJson(userFinal)));
                }
            });
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
            if (chat_id != 0) {

                if (year.format(dateTs).equals(year.format(dateCurr))) {
                    if ((day.format(dateTs).equals(day.format(dateCurr))) && (month.format(dateTs).equals(month.format(dateCurr)))) {
                        holder.time.setText(user.getFirst_name() + " " + user.getLast_name() + ", " + time_time);
                    } else {
                        holder.time.setText(user.getFirst_name() + " " + user.getLast_name() + ", " + time_day + " "
                                + convertMonth(Integer.parseInt(month.format(dateTs))));
                    }
                } else {
                    holder.time.setText(user.getFirst_name() + " " + user.getLast_name() + ", " + time_year);
                }
            } else {

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
            }
            if (user.getOnline() == 0) {
                holder.online.setVisibility(View.INVISIBLE);
            } else {
                holder.online.setVisibility(View.VISIBLE);
            }
            holder.body.addAutoLinkMode(AutoLinkMode.MODE_URL);
            holder.body.setUrlModeColor(Color.rgb(0, 200, 250));
            holder.body.setAutoLinkOnClickListener(new AutoLinkOnClickListener() {
                @Override
                public void onAutoLinkTextClick(AutoLinkMode autoLinkMode, String matchedText) {
                    if (AutoLinkMode.MODE_URL.equals(autoLinkMode)) {
                        Log.wtf("motya", matchedText);
                        while (matchedText.contains(" ")) {
                            matchedText = matchedText.replace(" ", "");
                        }
                        while (matchedText.contains("\n")) {
                            matchedText = matchedText.replace("\n", "");
                        }
                        Log.wtf("motya", "fix_matchetText=" + matchedText);
                        Util.goToUrl(DialogMessageActivity.this, matchedText);
                    }
                }
            });
            String bodyContainer = dialog.getBody();
            Log.wtf ("ooo",bodyContainer);

            if (dialog.getFwd_messages().size() != 0) {
                LayoutInflater inflater = getLayoutInflater();
                View cont = inflater.inflate(R.layout.attachment_conteiner_dinamic, null);
                TextView text = (TextView) cont.findViewById(R.id.textView3);
                text.setTextColor(Color.BLUE);
                text.setText(getString(R.string.FORWARD_MESSAGES));
                holder.line.addView(cont);
                text.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        frwd=true;
                        ArrayList<Dialogs> fwrd = new ArrayList<>();
                        for (int i = 0; i < dialog.getFwd_messages().size(); i++)
                            fwrd.add(dialog.getFwd_messages().get(i));
                        fwd_mess.add(fwrd);
                        pos.add(finalPos);
                        items = fwd_mess.get(fwd_mess.size() - 1);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
            for (int i=0;i<dialog.getAttachments().size();i++){
                switch (dialog.getAttachments().get(i).getType()){
                    case "photo":{
                        if (preferencesManager.getSettingPhotoChatOn()) {
                            String photo = "";
                            String photomess = "";
                            if (dialog.getAttachments().get(i).getPhoto().getPhoto_1280() != null) {
                                photo = dialog.getAttachments().get(i).getPhoto().getPhoto_1280();
                                photomess = dialog.getAttachments().get(i).getPhoto().getPhoto_604();
                            } else {
                                if (dialog.getAttachments().get(i).getPhoto().getPhoto_807() != null) {
                                    photo = dialog.getAttachments().get(i).getPhoto().getPhoto_807();
                                    photomess = dialog.getAttachments().get(i).getPhoto().getPhoto_604();
                                } else {
                                    if (dialog.getAttachments().get(i).getPhoto().getPhoto_604() != null) {
                                        photomess = photo = dialog.getAttachments().get(i).getPhoto().getPhoto_604();
                                    } else {
                                        if (dialog.getAttachments().get(i).getPhoto().getPhoto_130() != null) {
                                            photomess = photo = dialog.getAttachments().get(i).getPhoto().getPhoto_130();
                                        } else {
                                            if (dialog.getAttachments().get(i).getPhoto().getPhoto_75() != null) {
                                                photomess = photo = dialog.getAttachments().get(i).getPhoto().getPhoto_75();
                                            }
                                        }
                                    }
                                }
                            }
                            LayoutInflater inflater = getLayoutInflater();
                            View cont = inflater.inflate(R.layout.attachment_conteiner_dinamic, null);
                            ImageView photochka = (ImageView) cont.findViewById(R.id.imageView);
                            TextView text = (TextView) cont.findViewById(R.id.textView3);
                            text.setText(getString(R.string.PHOTO));
                            final String finalPhoto = photo;
                            Picasso.with(DialogMessageActivity.this)
                                    .load(photomess)
                                    .placeholder(R.drawable.loadshort)
                                    .error(R.drawable.errorshort)
                                    .into(photochka);
                            holder.line.addView(cont);
                            photochka.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new ImageViewer.Builder(DialogMessageActivity.this, new String[]{finalPhoto} )
                                            .show();
                                }
                            });
                        }else{
                            bodyContainer += "\n"+getString(R.string.PHOTO);
                        }
                        break;
                    }
                    case "sticker":{
                        LayoutInflater inflater = getLayoutInflater();
                        View cont = inflater.inflate(R.layout.attachment_conteiner_dinamic, null);
                        ImageView photochka = (ImageView) cont.findViewById(R.id.imageView);
                        TextView text = (TextView) cont.findViewById(R.id.textView3);
                        text.setText(R.string.STICKER);
                        Picasso.with(DialogMessageActivity.this)
                                .load(dialog.getAttachments().get(i).getSticker().getPhoto_256())
                                .placeholder(R.drawable.loadshort)
                                .error(R.drawable.errorshort)
                                .resize(200, 200)
                                .centerCrop()
                                .into(photochka);
                        holder.line.addView(cont);
                        break;
                    }
                    case "link":{
                        bodyContainer +="\n" + dialog.getAttachments().get(i).getLink().getUrl();
                        break;
                    }
                    case "video":{
                        LayoutInflater inflater = getLayoutInflater();
                        View cont = inflater.inflate(R.layout.attachment_conteiner_dinamic, null);
                        ImageView photochka = (ImageView) cont.findViewById(R.id.imageView);
                        TextView text = (TextView) cont.findViewById(R.id.textView3);
                        text.setText(dialog.getAttachments().get(i).getVideo().getTitle());
                        Picasso.with(DialogMessageActivity.this)
                                .load(dialog.getAttachments().get(i).getVideo().getPhoto_320())
                                .placeholder(R.drawable.loadshort)
                                .error(R.drawable.errorshort)
                                .resize(400, 300)
                                .centerCrop()
                                .into(photochka);
                        holder.line.addView(cont);
                        final String video = dialog.getAttachments().get(i).getVideo().getOwner_id() + "_" + dialog.getAttachments().get(i).getVideo().getId() + "_" + dialog.getAttachments().get(i).getVideo().getAccess_key();
                        photochka.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getString(R.string.LOADING), Toast.LENGTH_LONG);
                                toast.show();
                                String TOKEN = preferencesManager.getToken();
                                Call<ServerResponse<ItemMess<ArrayList<VideoInformation>>>> call = service.getVideos(TOKEN, video);

                                call.enqueue(new Callback<ServerResponse<ItemMess<ArrayList<VideoInformation>>>>() {
                                    @Override
                                    public void onResponse(Call<ServerResponse<ItemMess<ArrayList<VideoInformation>>>> call, Response<ServerResponse<ItemMess<ArrayList<VideoInformation>>>> response) {
                                        Log.wtf("motya", response.raw().toString());
                                        String res = response.body().getResponse().getitem().get(0).getPlayer();
                                        Uri address = Uri.parse(res);
                                        Intent openlink = new Intent(Intent.ACTION_VIEW, address);
                                        startActivity(openlink);
                                    }

                                    @Override
                                    public void onFailure(Call<ServerResponse<ItemMess<ArrayList<VideoInformation>>>> call, Throwable t) {
                                        Toast toast = Toast.makeText(getApplicationContext(),
                                                getString(R.string.LOST_INTERNET_CONNECTION), Toast.LENGTH_SHORT);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                    }
                                });
                            }
                        });
                        break;
                    }
                    case "doc":{
                        LayoutInflater inflater = getLayoutInflater();
                        View cont = inflater.inflate(R.layout.attachment_conteiner_dinamic, null);
                        ImageView photochka = (ImageView) cont.findViewById(R.id.imageView);
                        TextView text = (TextView) cont.findViewById(R.id.textView3);
                        text.setText(dialog.getAttachments().get(i).getDoc().getTitle());
                        if (dialog.getAttachments().get(i).getDoc().getType() == 1) {
                            Picasso.with(DialogMessageActivity.this)
                                    .load(R.drawable.doc)
                                    .resize(150, 150)
                                    .centerCrop()
                                    .into(photochka);
                        } else {
                            Picasso.with(DialogMessageActivity.this)
                                    .load(R.drawable.zip)
                                    .resize(150, 150)
                                    .centerCrop()
                                    .into(photochka);
                        }
                        holder.line.addView(cont);
                        final Attachment att = dialog.getAttachments().get(i);
                        photochka.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String res = att.getDoc().getUrl();
                                Uri address = Uri.parse(res);
                                Intent openlink = new Intent(Intent.ACTION_VIEW, address);
                                startActivity(openlink);
                            }
                        });
                        break;
                    }
                    case "audio":{
                        LayoutInflater inflater = getLayoutInflater();
                        View cont = inflater.inflate(R.layout.attachment_conteiner_audio_dinamic, null);
                        TextView text = (TextView) cont.findViewById(R.id.textView);
                        text.setText(dialog.getAttachments().get(i).getAudio().getArtist() + " - " + dialog.getAttachments().get(i).getAudio().getTitle());
                        Button button = (Button) cont.findViewById(R.id.button);
                        Button button1 = (Button) cont.findViewById(R.id.button1);
                        Button button2 = (Button) cont.findViewById(R.id.button2);
                        Button button3 = (Button) cont.findViewById(R.id.button3);
                        Button button4 = (Button) cont.findViewById(R.id.button4);
                        holder.line.addView(cont);
                        final String url = dialog.getAttachments().get(i).getAudio().getUrl();
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (DialogsActivity.mediaPlayer != null) {
                                    DialogsActivity.mediaPlayer.seekTo(DialogsActivity.mediaPlayer.getCurrentPosition() - 5000);
                                }
                            }
                        });
                        button1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (DialogsActivity.mediaPlayer != null) {
                                    if (DialogsActivity.mediaPlayer.getCurrentPosition() == 0) {
                                        try {
                                            DialogsActivity.mediaPlayer.release();
                                            DialogsActivity.mediaPlayer = null;
                                            DialogsActivity.mediaPlayer = new MediaPlayer();
                                            DialogsActivity.mediaPlayer.setDataSource(url);
                                            DialogsActivity.mediaPlayer.prepare();
                                            DialogsActivity.mediaPlayer.start();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        DialogsActivity.mediaPlayer.start();
                                    }
                                } else {
                                    try {
                                        DialogsActivity.mediaPlayer = new MediaPlayer();
                                        DialogsActivity.mediaPlayer.setDataSource(url);
                                        DialogsActivity.mediaPlayer.prepare();
                                        DialogsActivity.mediaPlayer.start();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        });
                        button2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (DialogsActivity.mediaPlayer != null) {
                                    DialogsActivity.mediaPlayer.pause();
                                }
                            }
                        });
                        button3.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (DialogsActivity.mediaPlayer != null) {
                                    DialogsActivity.mediaPlayer.seekTo(DialogsActivity.mediaPlayer.getCurrentPosition() + 5000);
                                }
                            }
                        });
                        button4.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (DialogsActivity.mediaPlayer != null) {
                                    DialogsActivity.mediaPlayer.pause();
                                    DialogsActivity.mediaPlayer.seekTo(0);
                                }
                            }
                        });
                        break;
                    }
                    case "wall":{
                        bodyContainer += "\n" + dialog.getAttachments().get(i).getType();
                        break;
                    }
                    case "gift":{
                        LayoutInflater inflater = getLayoutInflater();
                        View cont = inflater.inflate(R.layout.attachment_conteiner_dinamic, null);
                        ImageView photochka = (ImageView) cont.findViewById(R.id.imageView);
                        TextView text = (TextView) cont.findViewById(R.id.textView3);
                        text.setText(R.string.GIFT);
                        Picasso.with(DialogMessageActivity.this)
                                .load(dialog.getAttachments().get(i).getGift().getThumb_256())
                                .placeholder(R.drawable.loadshort)
                                .error(R.drawable.errorshort)
                                .resize(200, 200)
                                .centerCrop()
                                .into(photochka);
                        holder.line.addView(cont);
                        break;
                    }
                }

            }
            holder.body.setAutoLinkText(bodyContainer);
            if (dialog.getAction()!=null){
                if (dialog.getAction().equals("chat_kick_user"))
                holder.body.setAutoLinkText(getString(R.string.left_chat));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
