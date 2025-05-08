package com.video.vidbr;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

public class RateUsDialog extends Dialog {
    private float userRate = 0;

    public RateUsDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rate_us_dialog_layout);

        final AppCompatButton rateNowBtn = findViewById(R.id.rateNowBtn);
        final AppCompatButton laterBtn = findViewById(R.id.laterBtn);
        final RatingBar ratingBar = findViewById(R.id.ratingBar);
        final TextView ratingEmoji = findViewById(R.id.ratingEmoji); // TextView for emojis

        rateNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                openPlayStore();
            }
        });

        laterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                // Update the TextView with the corresponding emoji based on the rating
                if (rating <= 1) {
                    ratingEmoji.setText("😡"); // 1-star emoji
                } else if (rating <= 2) {
                    ratingEmoji.setText("☹️"); // 2-star emoji
                } else if (rating <= 3) {
                    ratingEmoji.setText("😐"); // 3-star emoji
                } else if (rating <= 4) {
                    ratingEmoji.setText("😊"); // 4-star emoji
                } else if (rating <= 5) {
                    ratingEmoji.setText("😍"); // 5-star emoji
                }

                animateEmoji(ratingEmoji);
                userRate = rating;
            }
        });
    }

    // Animation for the TextView containing emojis
    private void animateEmoji(TextView ratingEmoji) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0, 1, // fromX, toX
                0, 1, // fromY, toY
                Animation.RELATIVE_TO_SELF, 0.5f, // pivotXType, pivotXValue
                Animation.RELATIVE_TO_SELF, 0.5f  // pivotYType, pivotYValue
        );
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setDuration(200);
        ratingEmoji.startAnimation(scaleAnimation);
    }

    // Open Play Store to rate the app
    private void openPlayStore() {
        Context context = getContext();
        try {
            // Try to open the Play Store app
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + context.getPackageName())));
        } catch (ActivityNotFoundException e) {
            // If Play Store app is not installed, open Play Store in browser
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }
}
