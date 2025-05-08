package com.video.vidbr.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class CategoryAdapter extends ArrayAdapter<String> {

    public CategoryAdapter(Context context, List<String> categories) {
        super(context, 0, categories);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(getItem(position));
        textView.setTextColor(Color.BLACK); // Set the text color here
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Define o tamanho do texto para 14sp

        return convertView;
    }
}
