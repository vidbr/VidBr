package com.video.vidbr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

public class ThumbnailBottomSheetFragment extends BottomSheetDialogFragment {

    private String videoPath;
    private String thumbnailPath;
    public ThumbnailBottomSheetFragment(String videoPath, String thumbnailPath) {
        this.videoPath = videoPath;
        this.thumbnailPath = thumbnailPath;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog); // Apply the custom style
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_thumbnail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView thumbnailImage = view.findViewById(R.id.thumbnail_image);
        TextView thumbnailMessage = view.findViewById(R.id.thumbnail_message);
        Button shareButton = view.findViewById(R.id.share_button);

        thumbnailMessage.setText(getString(R.string.download_concluido));

        if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
            File thumbnailFile = new File(thumbnailPath);
            if (thumbnailFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath);
                if (bitmap != null) {
                    thumbnailImage.setImageBitmap(bitmap);
                } else {
                    thumbnailImage.setImageResource(R.drawable.ic_launcher_background); // Default image if bitmap is null
                }
            } else {
                thumbnailImage.setImageResource(R.drawable.ic_launcher_background); // Default image if file does not exist
            }
        } else {
            thumbnailImage.setImageResource(R.drawable.ic_launcher_background); // Default image if path is null or empty
        }

        shareButton.setOnClickListener(v -> shareVideo(videoPath));
    }

    private void shareVideo(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            Toast.makeText(getContext(), "No video path provided!", Toast.LENGTH_SHORT).show();
            return;
        }

        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            Toast.makeText(getContext(), "Video file does not exist!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri videoUri = FileProvider.getUriForFile(requireContext(), "com.video.vidbr.fileprovider", videoFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(shareIntent, "Share video via"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "No app available to share video.", Toast.LENGTH_SHORT).show();
        }
    }

}
