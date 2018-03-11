package org.mult.daap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.background.JmDNSListener;
import org.mult.daap.background.LoginManager;
import org.mult.daap.background.WrapMulticastLock;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.entity.ServerEntity;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class AddServerMenu extends AppCompatActivity implements Observer {
    private JmDNSListener jmDNSListener;
    private static final int PASSWORD_DIALOG = 0;
    private static final int MENU_ABOUT = 1;
    private static final int MENU_DONATE = 2;
    private static final int MENU_PREFS = 3;
    private Builder builder;
    private ProgressDialog progressDialog;
    private WrapMulticastLock fLock;
    private ServerAdapter discoveredServersListViewAdapter;
    private final List<DiscoveredServer> discoveredServers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_server_menus);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        builder = new AlertDialog.Builder(this);
        Button okButton = findViewById(R.id.serverOkButton);
        okButton.setEnabled(false);
        EditText serverAddressEditText = findViewById(R.id.serverUrlText);
        serverAddressEditText.addTextChangedListener(new tw(okButton));
        CheckBox loginCheckBox = findViewById(R.id.loginCheckBox);
        loginCheckBox.setOnCheckedChangeListener(new loginRequiredListener(findViewById(R.id.passwordSection)));
        okButton.setEnabled(false);
        okButton.setOnClickListener(new AddServerButtonListener(
                this.getApplicationContext(),
                serverAddressEditText,
                (EditText)findViewById(R.id.serverPasswordText),
                (EditText)findViewById(R.id.serverPortText),
                loginCheckBox));
        ListView discoveredServersListView = findViewById(R.id.discoveredServersListView);
        discoveredServersListViewAdapter = new ServerAdapter();
        discoveredServersListView.setAdapter(discoveredServersListViewAdapter );
        discoveredServersListView.setOnItemClickListener(discoveredServerClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        boolean wiFi = false;
        if (wifiManager != null) {
            wiFi = wifiManager.isWifiEnabled();
        }
        if (wiFi) {
            try {
                wiFi = true;
                fLock = new WrapMulticastLock(wifiManager);
                fLock.getInstance().acquire();
                byte[] wifiAddress = intToIp(wifiManager.getDhcpInfo().ipAddress);
                InetAddress wifi = InetAddress.getByAddress(wifiAddress);
                jmDNSListener = new JmDNSListener(new JmDNSHandler(this), wifi);
                jmDNSListener.start();
            } catch (UnknownHostException e) {
                wiFi = false;
            }
        }
        if (!wiFi) {
            findViewById(R.id.discoveredServersSection).setVisibility(View.GONE);
        } else {
            LoginManager lm = Contents.loginManager;
            if (lm != null) {
                lm.addObserver(this);
                // Since lm is not null, we have to create a new pd
                int lastMessage = lm.getLastMessage();
                update(lm, LoginManager.INITIATED);
                if (lastMessage != LoginManager.INITIATED) {
                    update(lm, lastMessage);
                }

                if (Intent.ACTION_VIEW.equals(getIntent().getAction())
                        || Intent.ACTION_PICK.equals(getIntent().getAction())) {
                    try {
                        Uri uri = getIntent().getData();
                        getIntent().setData(null);
                        String password = uri.getFragment();
                        if (password == null) {
                            lm = new LoginManager(uri.getHost(), "", false);
                            startLogin(lm);
                        } else {
                            Log.d("Servers", "host = (" + uri.getHost() + ")");
                            Log.d("Servers", "password = (" + password + ")");
                            lm = new LoginManager(uri.getHost(), password, true);
                            startLogin(lm);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (jmDNSListener != null) {
            jmDNSListener.interrupt();
            jmDNSListener = null;
        }
        if (fLock != null) {
            fLock.getInstance().release();
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (Contents.loginManager != null) {
            Contents.loginManager.deleteObserver(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ABOUT, 0, R.string.about_info).setIcon(
                R.drawable.ic_menu_about);
        menu.add(0, MENU_DONATE, 0, R.string.donate).setIcon(
                R.drawable.ic_menu_send);
        menu.add(0, MENU_PREFS, 0, getString(R.string.preferences)).setIcon(
                android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Builder builder = new AlertDialog.Builder(this);
        Intent intent;
        switch (item.getItemId()) {
            case MENU_ABOUT:
                builder.setTitle(getString(R.string.about_dialog_title));
                builder.setMessage(getString(R.string.info));
                builder.setPositiveButton(getString(android.R.string.ok), null);
                builder.show();
                return true;
            case MENU_PREFS:
                intent = new Intent(AddServerMenu.this, Preferences.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_DONATE:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=chrismiceli%40gmail%2ecom&lc=US&item_name=DAAP%20%2d%20Android%20Application&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
                startActivityForResult(intent, 1);
        }
        return false;
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case PASSWORD_DIALOG:
                dialog = new Dialog(this);
                dialog.setContentView(R.layout.password_prompt);
                dialog.setTitle(getString(R.string.password));
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                Button buttonConfrim = dialog.findViewById(R.id.PasswordOkButton);
                Button buttonCancel = dialog.findViewById(R.id.PasswordCancelButton);
                final EditText password = dialog.findViewById(R.id.PasswordEditText);
                buttonConfrim.setOnClickListener(new android.view.View.OnClickListener() {
                            public void onClick(View arg0) {
                                Contents.loginManager.interrupt();
                                Contents.loginManager.deleteObservers();
                                LoginManager lm = new LoginManager(
                                        Contents.loginManager.address, password
                                        .getText().toString(), true);
                                password.setText("");
                                startLogin(lm);
                                dismissDialog(PASSWORD_DIALOG);
                            }
                        });
                buttonCancel.setOnClickListener(new android.view.View.OnClickListener() {
                            public void onClick(View v) {
                                Contents.loginManager = null;
                                password.setText("");
                                dismissDialog(PASSWORD_DIALOG);
                            }
                        });
                break;
            default:
                dialog = null;
                break;
        }

        return dialog;
    }

    private void addDiscoveredServer(String name, String address) {
        DiscoveredServer discoveredServer = new DiscoveredServer();
        discoveredServer.name = name;
        discoveredServer.address = address;
        discoveredServers.add(discoveredServer);
        findViewById(R.id.searchingProgressBar).setVisibility(View.GONE);
        discoveredServersListViewAdapter.notifyDataSetChanged();
    }

    public void update(Observable observable, Object data) {
        int state = (int) data;
        if (state == LoginManager.INITIATED) {
            progressDialog = ProgressDialog.show(this,
                    getString(R.string.connecting_title),
                    getString(R.string.connecting_detail), true, true);
            DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    if (Contents.loginManager != null) {
                        Contents.loginManager.interrupt();
                        Contents.loginManager.deleteObservers();
                        Contents.loginManager = null;
                    }
                }
            };
            progressDialog.setOnCancelListener(onCancelListener);
        } else if (state == LoginManager.CONNECTION_FINISHED) {
            progressDialog.dismiss();
            // save the server
            boolean loginRequired = Contents.loginManager.password
                    .length() != 0;
            saveServer(Contents.loginManager.address, Contents.loginManager.password, loginRequired);
            Contents.loginManager = null;
            final Intent intent = new Intent(
                    AddServerMenu.this,
                    PlaylistBrowser.class);
            startActivityForResult(intent, 1);
        } else if (state == LoginManager.PASSWORD_FAILED) {
            progressDialog.dismiss();
            showDialog(PASSWORD_DIALOG);
        } else {
            // ERROR
            progressDialog.dismiss();
            Contents.loginManager = null;
            Toast tst = Toast.makeText(
                    AddServerMenu.this,
                    getString(R.string.unable_to_connect),
                    Toast.LENGTH_LONG);
            tst.setGravity(
                    Gravity.CENTER,
                    tst.getXOffset() / 2,
                    tst.getYOffset() / 2);
            tst.show();
            Contents.loginManager = null;
        }
    }

    private void saveServer(String serverAddress, String password, boolean loginCheckBox) {
        ServerDao serverDao = AppDatabase.getInstance(this.getApplicationContext()).serverDao();
        List<ServerEntity> serverList = new ArrayList<>();
        serverList.add(new ServerEntity(serverAddress, loginCheckBox ? password : null));
        serverDao.insertAll(serverList);
    }

    private byte[] intToIp(int i) {
        byte[] res = new byte[4];
        res[0] = (byte) (i & 0xff);
        res[1] = (byte) ((i >> 8) & 0xff);
        res[2] = (byte) ((i >> 16) & 0xff);
        res[3] = (byte) ((i >> 24) & 0xff);
        return res;
    }

    private void startLogin(LoginManager lm) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        MediaPlayback.clearState();
        Contents.loginManager = lm;
        lm.addObserver(this);
        Thread thread = new Thread(lm);
        thread.start();
        update(lm, lm.getLastMessage());
    }

    private final AdapterView.OnItemClickListener discoveredServerClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            DiscoveredServer discoveredServer = discoveredServers.get(position);
            LoginManager lm = new LoginManager(
                    discoveredServer.address,
                    "",
                    false);
            startLogin(lm);
        }
    };

    private class DiscoveredServer {
        public String name;
        public String address;
    }

    class ServerAdapter extends BaseAdapter {
        @Override
        public View getView(final int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_complex, container);
            }
            DiscoveredServer discoveredServer = (DiscoveredServer)getItem(position);
            if (discoveredServer != null) {
                ((TextView)convertView.findViewById(R.id.list_complex_title)).setText(discoveredServer.name);
                ((TextView)convertView.findViewById(R.id.list_complex_caption)).setText(discoveredServer.address);
            }
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            if (position >= discoveredServers.size()) {
                return discoveredServers.get(position);
            }

            return null;
        }

        @Override
        public int getCount() {
            return discoveredServers.size();
        }
    }

    private static class JmDNSHandler extends Handler {
        private final WeakReference<AddServerMenu> addServerMenu;

        public JmDNSHandler(AddServerMenu addServerMenu) {
            this.addServerMenu = new WeakReference<>(addServerMenu);
        }
        @Override
        public void handleMessage(Message message) {
            if (addServerMenu.get() != null) {
                Bundle bundle = message.getData();
                String name = bundle.getString("name");
                String address = bundle.getString("address");
                addServerMenu.get().addDiscoveredServer(name, address);
            }
        }
    }

    private class tw implements TextWatcher {
        private final Button okButton;

        tw(Button okButton) {
            this.okButton = okButton;
        }
        public void afterTextChanged(Editable s) {
            if (s.length() != 0) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }
    }

    private class loginRequiredListener implements OnCheckedChangeListener {
        private final View passwordSection;

        loginRequiredListener(View passwordSection) {
            this.passwordSection = passwordSection;
            passwordSection.setVisibility(View.GONE);
        }
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (isChecked) {
                passwordSection.setVisibility(View.VISIBLE);
            } else {
                passwordSection.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Function that is called when the Add Server button is pressed
     */
    private class AddServerButtonListener implements OnClickListener {
        private final Context context;
        private final EditText serverAddressEditText;
        private final EditText passwordEditText;
        private final EditText serverPortEditText;
        private final CheckBox loginCheckBox;

        AddServerButtonListener(
                Context context,
                EditText serverAddressEditText,
                EditText passwordEditText,
                EditText serverPortEditText,
                CheckBox loginCheckBox) {
            this.context = context;
            this.serverAddressEditText = serverAddressEditText;
            this.passwordEditText = passwordEditText;
            this.serverPortEditText = serverPortEditText;
            this.loginCheckBox = loginCheckBox;
        }

        @SuppressLint("SetTextI18n")
        public void onClick(View v) {
            final String password = passwordEditText.getText().toString();
            String port = serverPortEditText.getText().toString();
            if (port.equals("")) {
                serverPortEditText.setText("3689");
                port = "3689";
            }
            if (loginCheckBox.isChecked() && password.equals("")) {
                builder.setTitle(R.string.error_title);
                builder.setMessage(R.string.add_server_error_message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                return;
            }
            final String serverAddress = serverAddressEditText.getText().toString() + ":" + port;
            final List<ServerEntity> servers = new ArrayList<>();
            servers.add(new ServerEntity(serverAddress, loginCheckBox.isChecked() ? password : null));
            new Thread(new Runnable() {
                public void run() {
                    ServerDao serverDao = AppDatabase.getInstance(context).serverDao();
                    serverDao.insertAll(servers);
                    LoginManager loginManager = new LoginManager(
                            serverAddress,
                            loginCheckBox.isChecked() ? password : null,
                            loginCheckBox.isChecked());
                    startLogin(loginManager);
                }
            }).start();
        }
    }
}