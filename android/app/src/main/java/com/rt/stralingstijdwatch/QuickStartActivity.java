package com.rt.stralingstijdwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Snelkoppeling-activity die direct de laatst ingestelde belichtingstimer start.
 * Kan gebruikt worden als App-snelkoppeling op een watchface of onder een fysieke knop.
 */
public class QuickStartActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("action", "start_timer");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
