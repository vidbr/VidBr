package com.video.vidbr;

import static androidx.fragment.app.DialogFragment.STYLE_NORMAL;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class NSFWAlertBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_LABEL = "label";
    private String explicitLabel;

    public static NSFWAlertBottomSheet newInstance(String label) {
        NSFWAlertBottomSheet fragment = new NSFWAlertBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_LABEL, label);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            explicitLabel = getArguments().getString(ARG_LABEL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nsfw_alert_bottom_sheet, container, false);

        TextView tvMessage = view.findViewById(R.id.tvMessage);
        tvMessage.setText(getString(R.string.inappropriate_content_message_param, explicitLabel));

        MaterialButton btnUnderstand = view.findViewById(R.id.btnUnderstand);
        btnUnderstand.setOnClickListener(v -> dismiss());

        return view;
    }
}