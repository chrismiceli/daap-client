package org.mult.daap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.mult.daap.background.JmDNSListener;
import org.mult.daap.background.WrapMulticastLock;
import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.PasswordFailedException;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.model.DiscoveredServer;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AddServerMenu extends AppCompatActivity {
    private JmDNSListener jmDNSListener;
    private static final int MENU_ABOUT = 0;
    private static final int MENU_DONATE = 1;
    private static final int MENU_PREFS = 2;
    private Builder builder;
    private WrapMulticastLock fLock;
    private ServerAdapter discoveredServersListViewAdapter;
    private final List<DiscoveredServer> discoveredServers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_add_server_menus);

        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        this.builder = new AlertDialog.Builder(this);

        Button addServerButton = this.findViewById(R.id.addServerButton);

        EditText serverAddressEditText = this.findViewById(R.id.serverUrlText);
        serverAddressEditText.addTextChangedListener(new DaapUrlTextWatcher(addServerButton));

        CheckBox loginCheckBox = this.findViewById(R.id.loginCheckBox);
        loginCheckBox.setOnCheckedChangeListener(new LoginRequiredListener(this.findViewById(R.id.passwordSection)));

        addServerButton.setOnClickListener(new AddServerButtonListener(
                this.getApplication(),
                serverAddressEditText,
                (EditText) this.findViewById(R.id.serverPasswordText),
                (EditText) this.findViewById(R.id.serverPortText),
                loginCheckBox));

        ListView discoveredServersListView = this.findViewById(R.id.discoveredServersListView);
        discoveredServersListViewAdapter = new ServerAdapter();
        discoveredServersListView.setAdapter(discoveredServersListViewAdapter);
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
            this.findViewById(R.id.discoveredServersSection).setVisibility(View.GONE);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ABOUT, 0, getString(R.string.about_info)).setIcon(
                R.drawable.ic_menu_about);
        menu.add(0, MENU_DONATE, 0, getString(R.string.donate)).setIcon(
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

    private void addDiscoveredServer(String name, String address) {
        DiscoveredServer discoveredServer = new DiscoveredServer(name, address);
        discoveredServers.add(discoveredServer);
        findViewById(R.id.searchingProgressBar).setVisibility(View.GONE);
        discoveredServersListViewAdapter.notifyDataSetChanged();
    }

    private byte[] intToIp(int i) {
        byte[] res = new byte[4];
        res[0] = (byte) (i & 0xff);
        res[1] = (byte) ((i >> 8) & 0xff);
        res[2] = (byte) ((i >> 16) & 0xff);
        res[3] = (byte) ((i >> 24) & 0xff);
        return res;
    }

    private final AdapterView.OnItemClickListener discoveredServerClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DiscoveredServer discoveredServer = discoveredServers.get(position);
            new LoginManager(discoveredServer).execute();
        }
    };

    class ServerAdapter extends BaseAdapter {
        @Override
        public View getView(final int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_complex, container);
            }

            DiscoveredServer discoveredServer = (DiscoveredServer) getItem(position);
            if (discoveredServer != null) {
                ((TextView) convertView.findViewById(R.id.list_complex_title)).setText(discoveredServer.getName());
                ((TextView) convertView.findViewById(R.id.list_complex_caption)).setText(discoveredServer.getAddress());
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

    /**
     * A text watcher to enable/disable the 'add server' button depending on the server url text
     */
    private class DaapUrlTextWatcher implements TextWatcher {
        private final Button okButton;

        DaapUrlTextWatcher(Button okButton) {
            this.okButton = okButton;
        }

        @Override
        public void afterTextChanged(Editable textField) {
            if (textField.length() > 0) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * A listener for the login required checkbox.
     * When checked, a layout box will be shown to allow the user to enter the password.
     */
    private class LoginRequiredListener implements OnCheckedChangeListener {
        private final View passwordSection;

        LoginRequiredListener(View passwordSectionView) {
            this.passwordSection = passwordSectionView;
            this.passwordSection.setVisibility(View.GONE);
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                this.passwordSection.setVisibility(View.VISIBLE);
            } else {
                this.passwordSection.setVisibility(View.GONE);
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
                port = "3689";
                serverPortEditText.setText(port);
            }

            if (loginCheckBox.isChecked() && password.equals("")) {
                builder.setTitle(getString(R.string.error_title));
                builder.setMessage(getString(R.string.add_server_error_message));
                builder.setPositiveButton(getString(android.R.string.ok), null);
                builder.show();

                return;
            }

            final String serverAddress = serverAddressEditText.getText().toString() + ":" + port;
            ServerEntity server = new ServerEntity(serverAddress, loginCheckBox.isChecked() ? password : null);

            // login to the server, and update UI
            new LoginManager(server).execute();
        }
    }

    public class LoginManager extends AsyncTask<Void, Integer, Integer> {
        public final static int INITIATED = 1;
        public final static int CONNECTION_FINISHED = 1;
        public final static int ERROR = 2;
        public final static int PASSWORD_FAILED = 3;
        public final ServerEntity server;

        public LoginManager(ServerEntity server) {
            this.server = server;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Button addServerButton = findViewById(R.id.addServerButton);
            LinearLayout connectingSection = findViewById(R.id.connectingSection);
            ProgressBar progressBar = findViewById(R.id.progressBar);

            addServerButton.setEnabled(false);
            connectingSection.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // login to the DAAP server, if successful server is stored in database
            try {
                String[] urlAddress = this.server.getAddress().split(":");
                String hostname = urlAddress[0];

                Contents.address = InetAddress.getByName(hostname);

                if (Contents.daapHost != null) {
                    try {
                        Contents.daapHost.logout();
                        Contents.playlist_id = -1;
                    } catch (Exception e) {
                        // do nothing
                    }

                    Contents.clearLists();
                }

                if (server.loginRequired()) {
                    Contents.daapHost = new Host(this.server.getPassword(), Contents.address, this.server.getPort());
                } else {
                    Contents.daapHost = new Host(null, Contents.address, this.server.getPort());
                }

                try {
                    Contents.daapHost.connect();
                } catch (PasswordFailedException e) {
                    publishProgress(LoginManager.PASSWORD_FAILED);
                    return LoginManager.PASSWORD_FAILED;
                }
            } catch (Exception e) {
                publishProgress(LoginManager.ERROR);
                return LoginManager.ERROR;
            }

            publishProgress(LoginManager.CONNECTION_FINISHED);
            return LoginManager.CONNECTION_FINISHED;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            int state = values[0];
            TextView progressMessage = findViewById(R.id.progressBarText);
            LinearLayout connectingSection = findViewById(R.id.connectingSection);
            ProgressBar progressBar = findViewById(R.id.progressBar);
            if (state == LoginManager.INITIATED) {
                connectingSection.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressMessage.setText(getString(R.string.connecting_title) + " - " + getString(R.string.connecting_detail));
            } else if (state == LoginManager.CONNECTION_FINISHED) {
                // success, save the server to the database
                ServerDao serverDao = AppDatabase.getInstance(AddServerMenu.this).serverDao();
                serverDao.setDaapServer(server);

                connectingSection.setVisibility(View.GONE);
            } else if (state == LoginManager.PASSWORD_FAILED) {
                progressBar.setVisibility(View.INVISIBLE);
                progressMessage.setText(getString(R.string.login_required));
            } else {
                // error connecting
                progressBar.setVisibility(View.INVISIBLE);
                progressMessage.setText(getString(R.string.unable_to_connect));
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == LoginManager.CONNECTION_FINISHED) {
                final Intent intent = new Intent(AddServerMenu.this, PlaylistBrowser.class);
                startActivityForResult(intent, 1);
            } else {
                Button addServerButton = findViewById(R.id.addServerButton);
                addServerButton.setEnabled(true);
            }
        }
    }
}