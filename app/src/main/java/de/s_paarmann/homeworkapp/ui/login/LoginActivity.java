/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui.login;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.data.IHWGroupMapping;
import de.mlessmann.api.data.IHWProvider;
import de.mlessmann.api.data.IHWUser;
import de.mlessmann.api.main.HWMgr;
import de.mlessmann.exceptions.StillConnectedException;
import de.s_paarmann.homeworkapp.AutomaticReminderManager;
import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.network.LoginManager;
import de.s_paarmann.homeworkapp.network.LoginResultListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

  private FrameLayout contentFrame;
  private LayoutInflater inflater;

  private HWMgr mgr;
  private List<IHWProvider> providers;

  private IHWProvider selectedProvider;
  private IHWGroupMapping groups;
  private String selectedGroup;
  private String selectedUser;
  private String password;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    inflater = getLayoutInflater();
    contentFrame = (FrameLayout) findViewById(R.id.content_login);

    loadProviders();
  }

  private void loadProviders() {
    mgr = new HWMgr();
    mgr.getAvailableProvidersOBJ(null).registerListener(future -> {
      IHWFuture<List<IHWProvider>> providerFuture = (IHWFuture<List<IHWProvider>>) future;

      if (providerFuture.errorCode() == IHWFuture.ERRORCodes.OK) {
        providers = providerFuture.get();
        runOnUiThread(this::displayProviderSelect);
      } else {
        // TODO
        Toast.makeText(this, "Liste von Schulen konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
      }
    });
  }

  private void displayProviderSelect() {
    findViewById(R.id.login_loadingIcon).setVisibility(View.GONE);

    inflater.inflate(R.layout.login_provider_select, contentFrame, true);

    ListView lsProviders = (ListView) contentFrame.findViewById(R.id.lsViewProviders);

    List<String> providerNames = new ArrayList<>(providers.size());
    for (IHWProvider provider : providers) {
      providerNames.add(provider.getName());
    }

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                                                      providerNames);
    lsProviders.setAdapter(adapter);

    lsProviders.setOnItemClickListener((parent, view, position, id) -> {
      selectedProvider = providers.get(position);

      loadGroups();
    });
  }

  private void loadGroups() {
    findViewById(R.id.login_loadingIcon).setVisibility(View.VISIBLE);

    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_provider));

    try {
      mgr.setProvider(selectedProvider);
    } catch (StillConnectedException e) {
      mgr.release(true);
      try {
        mgr.setProvider(selectedProvider);
      } catch (StillConnectedException e2) {
        // TODO
        Toast.makeText(this, "Fehler beim Verbinden zum Server.", Toast.LENGTH_LONG).show();
        return;
      }
    }

    try {
      mgr.connect().registerListener(connFuture -> {
        if (connFuture.errorCode() == IHWFuture.ERRORCodes.OK) {
          mgr.isCompatible().registerListener(compFuture -> {
            if (((boolean)compFuture.getOrElse(Boolean.FALSE))) {
              mgr.getGroups("").registerListener(getGrpFuture -> {
                if (getGrpFuture.errorCode() == IHWFuture.ERRORCodes.OK) {
                  groups = ((IHWFuture<IHWGroupMapping>) getGrpFuture).get();

                  runOnUiThread(() -> {
                    displayGroupSelect();
                  });
                } else {
                  runOnUiThread(() -> {
                    // TODO
                    Toast.makeText(this, "Fehler beim Laden der Klassen.", Toast.LENGTH_LONG).show();
                  });
                }
              });
            } else {
              runOnUiThread(() -> {
                // TODO
                Toast.makeText(this, "Server hat eine inkompatible Version.", Toast.LENGTH_LONG).show();
              });
            }
          });
        } else {
          runOnUiThread(() -> {
            // TODO
            Toast.makeText(this, "Fehler beim Verbinden zum Server.", Toast.LENGTH_LONG).show();
          });
        }
      });
    } catch (StillConnectedException e) {
      // Should be utterly impossible
    }
  }

  private void displayGroupSelect() {
    findViewById(R.id.login_loadingIcon).setVisibility(View.GONE);

    inflater.inflate(R.layout.login_group_select, contentFrame, true);

    TextView txtProvider = (TextView) contentFrame.findViewById(R.id.txtLoginGroupSelectProvider);
    txtProvider.setText(selectedProvider.getName());

    ListView lsGroups = (ListView) contentFrame.findViewById(R.id.lsViewGroups);

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                                                      groups.getGroups());
    lsGroups.setAdapter(adapter);

    lsGroups.setOnItemClickListener((parent, view, position, id) -> {
      selectedGroup = groups.getGroups().get(position);

      displayUserSelect();
    });
  }

  private void displayUserSelect() {
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_group));

    inflater.inflate(R.layout.login_user_select, contentFrame, true);

    TextView txtProvider = (TextView) contentFrame.findViewById(R.id.txtLoginUserSelectProvider);
    TextView txtGroup = (TextView) contentFrame.findViewById(R.id.txtLoginUserSelectGroup);

    txtProvider.setText(selectedProvider.getName());
    txtGroup.setText(selectedGroup);

    ListView lsUsers = (ListView) contentFrame.findViewById(R.id.lsViewUsers);

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                                                      groups.getUsersFor(selectedGroup));
    lsUsers.setAdapter(adapter);

    lsUsers.setOnItemClickListener((parent, view, position, id) -> {
      selectedUser = groups.getUsersFor(selectedGroup).get(position);

      displayPasswordForm();
    });
  }

  private void displayPasswordForm() {
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_user));

    inflater.inflate(R.layout.login_password_form, contentFrame, true);

    TextView txtProvider = (TextView) contentFrame.findViewById(R.id.txtLoginPasswordFormProvider);
    TextView txtGroup = (TextView) contentFrame.findViewById(R.id.txtLoginPasswordFormGroup);
    TextView txtUser = (TextView) contentFrame.findViewById(R.id.txtLoginPasswordFormUser);

    txtProvider.setText(selectedProvider.getName());
    txtGroup.setText(selectedGroup);
    txtUser.setText(selectedUser);

    EditText txtPassword = (EditText) contentFrame.findViewById(R.id.txtLoginPasswordFormPassword);
    Button btnLogin = (Button) contentFrame.findViewById(R.id.btnLoginPasswordFormLogin);

    btnLogin.setOnClickListener(v -> {
      password = txtPassword.getText().toString();

      contentFrame.findViewById(R.id.content_login_password).setVisibility(View.GONE);
      contentFrame.findViewById(R.id.login_loadingIcon).setVisibility(View.VISIBLE);

      LoginManager.setCredentials(this, selectedProvider, selectedGroup, selectedUser, password);
      LoginManager.getHWMgr(this, (unused, result) -> {
        if (result == LoginResultListener.Result.LOGGED_IN) {
          runOnUiThread(() -> {
            Toast.makeText(this, "Erfolgreich eingeloggt.", Toast.LENGTH_SHORT).show();
            finish();
          });
        } else {
          runOnUiThread(() -> {
            // TODO
            Toast.makeText(this, "Fehler beim einloggen.", Toast.LENGTH_LONG).show();
            contentFrame.findViewById(R.id.login_loadingIcon).setVisibility(View.GONE);
            contentFrame.findViewById(R.id.content_login_password).setVisibility(View.VISIBLE);
          });
        }
      });
    });
  }
}
