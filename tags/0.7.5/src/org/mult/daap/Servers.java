package org.mult.daap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.mult.daap.background.DBAdapter;
import org.mult.daap.background.JmDNSListener;
import org.mult.daap.background.LoginManager;
import org.mult.daap.background.SeparatedListAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class Servers extends Activity implements Observer
{
    public final static String TITLE = "title";
    public final static String CAPTION = "caption";
    public final static String KEY = "key";
    private static final int MENU_ABOUT = 1;
    private static final int MENU_ADD = 2;
    private static final int CONTEXT_DELETE = 3;
    private static final int MENU_DONATE = 4;
    private static final int MENU_PREFS = 5;
    private static final String donateLink = new String(
            "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=chrismiceli%40gmail%2ecom&lc=US&item_name=DAAP%20%2d%20Android%20Application&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted");
    private ListView list = null;
    private static List<Map<String, ?>> localServers = null;
    private SeparatedListAdapter adapter = null;
    private DBAdapter db;
    @SuppressWarnings("unused")
    private JmDNSListener jmDNSListener = null;
    private String localLabel = null;
    private List<Map<String, String>> serversList = new ArrayList<Map<String, String>>();
    private ArrayList<Bundle> discoveredServers = new ArrayList<Bundle>();
    private ProgressDialog pd = null;

    public Map<String, ?> createItem(String title, String caption)
    {
        Map<String, String> item = new HashMap<String, String>();
        item.put(TITLE, title);
        item.put(CAPTION, caption);
        return item;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        localLabel = getString(R.string.local_servers);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        List<Map<String, ?>> rememberedServers = new LinkedList<Map<String, ?>>();
        jmDNSListener = new JmDNSListener(mDNSHandler);
        labelChanger.sendEmptyMessageDelayed(0, 1000);
        try
        {
            db = new DBAdapter(this);
            db.open();
            Cursor cursor = db.getAllServers();
            if (cursor.getCount() != 0)
            {
                cursor.moveToFirst();
                int nameIndex = cursor.getColumnIndexOrThrow("server_name");
                int addressIndex = cursor.getColumnIndexOrThrow("address");
                for (int i = 0; i < cursor.getCount(); i++)
                {
                    String name = cursor.getString(nameIndex);
                    String address = cursor.getString(addressIndex);
                    rememberedServers.add(createItem(name, address));
                    cursor.moveToNext();
                }
                cursor.close();
            }
            db.close();
        } catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            return;
        }
        rememberedServers.add(createItem(getString(R.string.add_server),
                getString(R.string.add_server_detail)));

        localServers = new LinkedList<Map<String, ?>>();
        adapter = new SeparatedListAdapter(this);
        adapter.addSection(getString(R.string.remembered_servers),
                new ServerAdapter(this, rememberedServers,
                        R.layout.list_complex, new String[] { TITLE, CAPTION },
                        new int[] { R.id.list_complex_title,
                                R.id.list_complex_caption }));
        adapter.addSection(localLabel,
                new ServerAdapter(this, localServers, R.layout.list_complex,
                        new String[] { TITLE, CAPTION }, new int[] {
                                R.id.list_complex_title,
                                R.id.list_complex_caption }));
        list = new ListView(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener(clickListener);
        list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo)
            {
                menu.setHeaderTitle(getString(R.string.options));
                menu.add(0, CONTEXT_DELETE, 0, R.string.delete_entry);
            }
        });
        this.setContentView(list);
        LoginManager lm = Contents.loginManager;
        if (lm != null)
        {
            lm.addObserver(this);
            // Since lm is not null, we have ot create a new pd
            Integer lastMessage = lm.getLastMessage();
            update(lm, LoginManager.INITIATED);
            if (lastMessage != LoginManager.INITIATED)
            {
                update(lm, lastMessage);
            }
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        labelChanger.removeMessages(0);
        labelChanger.removeMessages(1);
        labelChanger.removeMessages(2);
        if (pd != null)
        {
            pd.dismiss();
        }
        if (Contents.loginManager != null)
        {
            Contents.loginManager.deleteObserver(this);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem)
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        db = new DBAdapter(this);
        db.open();
        Cursor c = db.getAllServers();
        if (menuInfo.position - 1 >= c.getCount())
        {
            db.close();
            c.close();
            return true;
        }
        else
        {
            c.moveToFirst();
            for (int x = 0; x < menuInfo.position - 1; x++)
            {
                c.moveToNext();
            }
            int rowId = c.getInt(0);
            c.close();
            db.close();
            switch (aItem.getItemId())
            {
                case CONTEXT_DELETE:
                    db.open();
                    db.deleteServer(rowId);
                    Cursor profilesCursor = db.getAllServers();
                    int count = profilesCursor.getCount();
                    profilesCursor.close();
                    db.close();
                    if (count == 0)
                    {
                        db.open();
                        db.reCreate();
                        db.close();
                    }
                    final Intent intent = new Intent(Servers.this,
                            Servers.class);
                    startActivityForResult(intent, 1);
                    finish();
                    return true;
            }
        }
        return false;
    }

    private OnItemClickListener clickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id)
        {
            db.open();
            Cursor c = db.getAllServers();
            if (position - 1 >= c.getCount())
            {
                // We clicked a local server or add Server
                int count = c.getCount();
                c.close();
                db.close();
                if (position == count + 1)
                {
                    final Intent intent = new Intent(Servers.this,
                            AddServerMenu.class);
                    startActivityForResult(intent, 1);
                }
                else
                {
                    // We clicked one of the discovered servers
                    // 3 for remembered, add button, and local
                    Bundle specificBundle = discoveredServers.get(position
                            - count - 3);
                    LoginManager lm = new LoginManager(specificBundle
                            .getString("name"), specificBundle
                            .getString("address"), "", false);
                    startLogin(lm);
                }

            }
            else
            {
                c.moveToFirst();
                for (int i = 0; i < position - 1; i++)
                {
                    c.moveToNext();
                }
                LoginManager lm;
                if (c.getInt(c.getColumnIndex("login_required")) == 0)
                {
                    lm = new LoginManager(c.getString(c
                            .getColumnIndex("server_name")), c.getString(c
                            .getColumnIndex("address")), c.getString(c
                            .getColumnIndex("password")), false);
                }
                else
                {
                    lm = new LoginManager(c.getString(c
                            .getColumnIndex("server_name")), c.getString(c
                            .getColumnIndex("address")), c.getString(c
                            .getColumnIndex("password")), true);
                }
                c.close();
                db.close();
                startLogin(lm);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ADD, 0, getString(R.string.add_server)).setIcon(
                R.drawable.ic_menu_add);
        menu.add(0, MENU_ABOUT, 0, R.string.about_info).setIcon(
                R.drawable.ic_menu_about);
        menu.add(0, MENU_DONATE, 0, R.string.donate).setIcon(
                R.drawable.ic_menu_send);
        menu.add(0, MENU_PREFS, 0, getString(R.string.preferences)).setIcon(
                android.R.drawable.ic_menu_preferences);
        return true;
    }

    protected void startLogin(LoginManager lm)
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Contents.clearState();
        Contents.loginManager = lm;
        lm.addObserver(this);
        Thread thread = new Thread(lm);
        thread.start();
        update(lm, lm.getLastMessage());
    }

    public void update(Observable observable, Object data) {
      if (((Integer) data).compareTo(LoginManager.INITIATED) == 0) {
         pd = ProgressDialog.show(this, getString(R.string.connecting_title),
               getString(R.string.connecting_detail), true, true);
         OnCancelListener onCancelListener =  new OnCancelListener() {
            
            public void onCancel(DialogInterface dialog)
            {
                if(Contents.loginManager != null) {
                    Contents.loginManager.interrupt();
                    Contents.loginManager.deleteObservers();
                    Contents.loginManager = null;
                }
            }
        };
        pd.setOnCancelListener(onCancelListener );
      } else if (((Integer) data).compareTo(LoginManager.CONNECTION_FINISHED) == 0) {
         loginHandler.sendEmptyMessage(LoginManager.CONNECTION_FINISHED
               .intValue());
         return;
      } else {
         // ERROR
         Contents.loginManager.deleteObserver(this);
         loginHandler.sendEmptyMessage(LoginManager.ERROR.intValue());
         return;
      }
   }

    private Handler loginHandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            if (msg.what == LoginManager.CONNECTION_FINISHED.intValue())
            {

                pd.dismiss();
                Contents.loginManager = null;
                final Intent intent = new Intent(Servers.this,
                        PlaylistBrowser.class);
                startActivityForResult(intent, 1);
            }
            else
            {
                // ERROR
                pd.dismiss();
                Contents.loginManager = null;
                Toast tst = Toast.makeText(Servers.this,
                        getString(R.string.unable_to_connect),
                        Toast.LENGTH_LONG);
                tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                        .getYOffset() / 2);
                tst.show();
                Contents.loginManager = null;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Builder builder = new AlertDialog.Builder(this);
        Intent intent;
        switch (item.getItemId())
        {
            case MENU_ABOUT:
                builder.setTitle(getString(R.string.about_dialog_title));
                builder.setMessage(getString(R.string.info));
                builder.setPositiveButton(getString(android.R.string.ok), null);
                builder.show();
                return true;
            case MENU_PREFS:
                intent = new Intent(Servers.this, Preferences.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_ADD:
                intent = new Intent(Servers.this, AddServerMenu.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_DONATE:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(donateLink));
                startActivityForResult(intent, 1);
        }
        return false;
    }

    class ServerAdapter extends SimpleAdapter
    {
        class ViewWrapper
        {
            View base;
            TextView vwlcT = null;
            TextView vwlcC = null;
            ImageView vwimage = null;

            ViewWrapper(View base)
            {
                this.base = base;
            }

            TextView getlcT()
            { // list_complex Title
                if (vwlcT == null)
                {
                    vwlcT = (TextView) base
                            .findViewById(R.id.list_complex_title);
                }
                return vwlcT;
            }

            TextView getlcC()
            { // list_complex Caption
                if (vwlcC == null)
                {
                    vwlcC = (TextView) base
                            .findViewById(R.id.list_complex_caption);
                }
                return vwlcC;
            }
        }

        List<Map<String, ?>> mList;

        public ServerAdapter(Context context, List<Map<String, ?>> data,
                int resource, String[] from, int[] to)
        {
            super(context, data, resource, from, to);
            mList = data;
            Map<String, String> header = new HashMap<String, String>();
            header.put(TITLE, "Section Header");
            header.put(CAPTION, "header");
            header.put(KEY, "header_key");
            serversList.add(header);
            header.clear();
            for (int x = 0; x < data.size(); x++)
            {
                Map<String, String> item = new HashMap<String, String>();
                item.put(TITLE, (String) data.get(x).get(TITLE));
                item.put(CAPTION, (String) data.get(x).get(CAPTION));
                item.put(KEY, (String) data.get(x).get(KEY));
                serversList.add(item);
            }
        }

        public int getCount()
        {
            return mList.size();
        }

        public List<? extends Map<String, ?>> getList()
        {
            return mList;
        }

        public View getView(final int position, View convertView,
                ViewGroup parent)
        {
            View row = convertView;
            ViewWrapper wrapper = null;
            if (row == null)
            {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.list_complex, null);
                wrapper = new ViewWrapper(row);
                row.setTag(wrapper);
            }
            else
            {
                wrapper = (ViewWrapper) row.getTag();
            }
            wrapper.getlcT().setText((String) mList.get(position).get(TITLE));
            wrapper.getlcC().setText((String) mList.get(position).get(CAPTION));
            return (row);
        }
    }

    private Handler labelChanger = new Handler() {

        @Override
        public void handleMessage(Message msg)
        {
            localLabel = getString(R.string.local_servers);
            for (int i = 0; i < msg.what; i++)
            {
                localLabel = localLabel + ".";
            }
            adapter.headers.clear();
            adapter.headers.add(getString(R.string.remembered_servers));
            adapter.headers.add(localLabel);
            adapter.notifyDataSetChanged();
            labelChanger.sendEmptyMessageDelayed((msg.what + 1) % 4, 1000);
        }
    };

    private Handler mDNSHandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            Bundle bundle = (Bundle) msg.getData();
            String name = bundle.getString("name");
            String address = bundle.getString("address");
            localServers.add(createItem(name, address));
            discoveredServers.add(bundle);
            adapter.notifyDataSetChanged();
        }
    };
}