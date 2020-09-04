package org.mult.daap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import org.mult.daap.background.DBAdapter;

public class AddServerMenu extends Activity {
    private EditText serverNameEditText;
    private EditText serverAddressEditText;
    private EditText serverPortEditText;
    private EditText passwordEditText;
    private CheckBox loginCheckBox;
    private Button okButton;
    private DBAdapter db;
    private boolean creatingShortcut;
    Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_server_menu);
        builder = new AlertDialog.Builder(this);
        db = new DBAdapter(this);
        okButton = (Button) findViewById(R.id.serverOkButton);
        okButton.setEnabled(false);
        Button cancelButton = (Button) findViewById(R.id.serverCancelButton);
        serverNameEditText = (EditText) findViewById(R.id.serverNameText);
        serverAddressEditText = (EditText) findViewById(R.id.serverUrlText);
        serverPortEditText = (EditText) findViewById(R.id.serverPortText);
        loginCheckBox = (CheckBox) findViewById(R.id.loginCheckBox);
        passwordEditText = (EditText) findViewById(R.id.serverPasswordText);
        serverAddressEditText.addTextChangedListener(new tw());
        loginCheckBox.setOnCheckedChangeListener(new loginRequiredListener());
        okButton.setEnabled(false);
        passwordEditText.setEnabled(false);
        okButton.setOnClickListener(new AddServerButtonListener());
        cancelButton.setOnClickListener(new cancelButtonListener());
        creatingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent()
                .getAction())
                || Intent.ACTION_PICK.equals(getIntent().getAction());
    }

    private class tw implements TextWatcher {
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
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked == true) {
                passwordEditText.setEnabled(true);
            } else {
                passwordEditText.setEnabled(false);
            }
        }
    }

    /**
     * Function that is called when the Add Server button is pressed
     */
    private class AddServerButtonListener implements OnClickListener {
        /**
         * @param v The view that will be updated
         */
        public void onClick(View v) {
            String serverName = serverNameEditText.getText().toString();
            String serverAddress = serverAddressEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String port = serverPortEditText.getText().toString();
            if (port.equals("")) {
                serverPortEditText.setText("3689");
                port = "3689";
            }
            if (serverName.equals("") || serverAddress.equals("")) {
                builder.setTitle(R.string.error_title);
                builder.setMessage(R.string.add_server_error_message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                return;
            }
            if (loginCheckBox.isChecked() == true && password.equals("")) {
                builder.setTitle(R.string.error_title);
                builder.setMessage(R.string.add_server_error_message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                return;
            }
            serverAddress = serverAddress + ":" + port;
            db.open();
            if (!db.serverExists(serverName, serverAddress, password,
                    loginCheckBox.isChecked())) {
                db.insertServer(serverName, serverAddress, password,
                        loginCheckBox.isChecked());
            } else {
                Toast.makeText(AddServerMenu.this,
                        R.string.duplicate_server_error_title,
                        Toast.LENGTH_LONG).show();
            }
            db.close();
            if (creatingShortcut) {
                ShortcutIconResource icon = Intent.ShortcutIconResource
                        .fromContext(AddServerMenu.this, R.drawable.icon);
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("daap");
                builder.authority(serverAddress);
                if (loginCheckBox.isChecked() == true) {
                    builder.fragment(password);
                }
                // Log.d("AddServerMenu", "uri = (" + builder.build() + ")");
                Intent launchingIntent = new Intent(Intent.ACTION_VIEW,
                        builder.build());
                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchingIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, serverName);
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

                setResult(RESULT_OK, intent);
            } else {
                setResult(RESULT_OK);
            }
            finish();
        }
    }

    private class cancelButtonListener implements OnClickListener {
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
