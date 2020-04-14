package com.airmap.airmapsdktest.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdktest.R;

import java.util.UUID;

import timber.log.Timber;

public class AnonymousLoginDemoActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anon_login);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        statusTextView = findViewById(R.id.status_text_view);

        // check if user is already logged in
        if (!TextUtils.isEmpty(AirMap.getUserId())) {
            statusTextView.setText("Already logged in as:\n\n" + AirMap.getUserId() + "\n\nwith ability to create flights, receive traffic and send telemetry.");

            // if not use Anonymous Login
        } else {
            // Any unique identifier from the developer for their user (UUID, username, email)
            String userId = UUID.randomUUID().toString();

            AirMap.performAnonymousLogin(userId, new AirMapCallback<Void>() {
                @Override
                public void onSuccess(Void response) {
                    Timber.v("Token is: %s", AirMap.getAuthToken());
                    statusTextView.setText("Logged in as:\n\n" + AirMap.getUserId() + "\n\nNow able to create flights, receive traffic and send telemetry.");
                }

                @Override
                public void onError(AirMapException e) {
                    Timber.e(e, e.getDetailedMessage());
                    statusTextView.setText("Anonymous Login failed");
                }
            });
        }
    }
}
