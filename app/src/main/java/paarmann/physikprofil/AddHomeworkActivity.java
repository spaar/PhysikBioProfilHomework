/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.main.HWMgr;

import paarmann.physikprofil.network.HomeworkManager;

public class AddHomeworkActivity extends Activity {

  public static final String TAG = "AddHomeworkActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_homework);
    getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  public void onBtnAddHomeworkClick(View view) {
    String
        strDate =
        ((EditText) findViewById(R.id.txtDate)).getText()
            .toString(); // TODO: Make proper date selector
    String title = ((EditText) findViewById(R.id.txtTitle)).getText().toString();
    String subject = ((EditText) findViewById(R.id.txtSubject)).getText().toString();
    String desc = ((EditText) findViewById(R.id.txtDescription)).getText().toString();

    final HAElement element = new HAElement();
    element.date = strDate;
    element.title = title;
    element.subject = subject;
    element.desc = desc;

    HWMgr mgr = new HWMgr();

    HomeworkManager.addHomework(this, element, result -> {
      if (result == IHWFuture.ERRORCodes.OK) {
        runOnUiThread(() -> {
          Toast.makeText(this, "Hausaufgabe hinzugefügt.", Toast.LENGTH_SHORT).show();
        });
      } else {
        String msg;
        if (result == IHWFuture.ERRORCodes.INSUFFPERM) {
          msg = "Du bist nicht berechtigt Hausaufgaben hinzuzufügen.";
        } else {
          msg = "Unbekannter Fehler beim hinzufügen der Hausaufgabe.";
        }
        runOnUiThread(() -> {
          Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
      }
    });
  }
}
