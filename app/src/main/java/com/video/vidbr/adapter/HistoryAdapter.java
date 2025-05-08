package com.video.vidbr.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.video.vidbr.R;

import java.lang.reflect.Type;
import java.util.List;

public class HistoryAdapter extends ArrayAdapter<String> {

    private static final String PREFS_NAME = "SearchHistoryPrefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final int MAX_HISTORY_ITEMS = 10;

    private Context context;
    private List<String> searchHistory;
    private TextView viewMoreHistoryText;
    private boolean isFullHistoryVisible;

    public HistoryAdapter(@NonNull Context context, TextView viewMoreHistoryText) {
        super(context, 0);
        this.context = context;
        this.viewMoreHistoryText = viewMoreHistoryText;
        this.searchHistory = loadSearchHistory();
        this.isFullHistoryVisible = false;

        setViewMoreClickListener();
    }

    public void setFullHistoryVisible(boolean isVisible) {
        isFullHistoryVisible = isVisible;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_search_history, parent, false);
        }

        String originalHistoryItem = getItem(position);

        String historyItemToDisplay = originalHistoryItem;
        if (originalHistoryItem.length() > 30) {
            historyItemToDisplay = originalHistoryItem.substring(0, 30) + "...";
        }

        TextView historyItemName = convertView.findViewById(R.id.history_item_name);
        if (historyItemName != null) {
            historyItemName.setText(historyItemToDisplay);
        }

        ImageView historyItemClose = convertView.findViewById(R.id.history_item_close);
        if (historyItemClose != null) {
            historyItemClose.setOnClickListener(v -> {
                removeItemFromHistory(position);
            });
        }


        return convertView;
    }

    private List<String> loadSearchHistory() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonHistory = sharedPreferences.getString(KEY_SEARCH_HISTORY, "[]"); // Valor padrão vazio
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType(); // Tipo correto para a lista
        return gson.fromJson(jsonHistory, type);
    }

    private List<String> getLimitedHistory() {
        if (isFullHistoryVisible) {
            return searchHistory.size() > MAX_HISTORY_ITEMS ? searchHistory.subList(0, MAX_HISTORY_ITEMS) : searchHistory;
        } else {
            return searchHistory.size() > 4 ? searchHistory.subList(0, 4) : searchHistory;
        }
    }

    private void removeItemFromHistory(int position) {
        searchHistory.remove(position);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String jsonHistory = gson.toJson(searchHistory);
        editor.putString(KEY_SEARCH_HISTORY, jsonHistory);
        editor.apply();

        notifyDataSetChanged();
        updateViewMoreVisibility();
    }

    private void updateViewMoreVisibility() {
        if (isFullHistoryVisible) {
            viewMoreHistoryText.setText(context.getString(R.string.clear_all_history));
        } else {
            viewMoreHistoryText.setText(context.getString(R.string.view_more_history));
        }

        // Ajuste a visibilidade do botão baseado no tamanho do histórico
        if (searchHistory.size() > 4) {
            viewMoreHistoryText.setVisibility(View.VISIBLE);
        } else {
            viewMoreHistoryText.setVisibility(View.GONE);
        }
    }

    public void toggleHistoryView() {
        isFullHistoryVisible = !isFullHistoryVisible;
        searchHistory = getLimitedHistory();
        notifyDataSetChanged();
        updateViewMoreVisibility();
    }

    private void clearSearchHistory() {
        searchHistory.clear(); // Limpar a lista do histórico

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_SEARCH_HISTORY, "[]"); // Salvar lista vazia
        editor.apply();

        notifyDataSetChanged();

        updateViewMoreVisibility();
    }

    @Override
    public int getCount() {
        return getLimitedHistory().size();
    }

    @Override
    public String getItem(int position) {
        return getLimitedHistory().get(position);
    }

    public void setViewMoreClickListener() {
        viewMoreHistoryText.setOnClickListener(v -> {
            if (isFullHistoryVisible) {
                clearSearchHistory();
            } else {
                toggleHistoryView();
            }
        });
    }
}
