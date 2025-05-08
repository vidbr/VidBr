package com.video.vidbr;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            actionBar.setTitle("");
        }

        TextView termsLink = findViewById(R.id.terms_link);
        termsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String termsUrl = "https://bit.ly/vidbrtermsandconditions";
                Intent intent = new Intent(AboutActivity.this, WebViewActivity.class);
                intent.putExtra("url", termsUrl);
                startActivity(intent);
            }
        });

        TextView privacyPolicyLink = findViewById(R.id.privacy_policy_link);
        privacyPolicyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String privacyUrl = "https://bit.ly/vidbrprivacypolicy";
                Intent intent = new Intent(AboutActivity.this, WebViewActivity.class);
                intent.putExtra("url", privacyUrl);
                startActivity(intent);
            }
        });

        Button rateButton = findViewById(R.id.rate_button);
        rateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RateUsDialog rateUsDialog = new RateUsDialog(AboutActivity.this);
                rateUsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
                rateUsDialog.show();
            }
        });

        Button licensesButton = findViewById(R.id.licenses_button);
        licensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the LicencasActivity when the button is clicked
                Intent intent = new Intent(getApplicationContext(), LicencasActivity.class);
                startActivity(intent);
            }
        });

    }

    private void openWebPage(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
