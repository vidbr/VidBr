package com.video.vidbr.adapter;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.vidbr.R;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    private List<String> suggestions;
    private OnSuggestionClickListener listener;
    private String query; // To store the query text

    public SuggestionAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    // Set the query in the adapter and notify the change
    public void setQuery(String query) {
        this.query = query;
        notifyDataSetChanged();  // Notify the adapter to refresh the list
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String originalSuggestion = suggestions.get(position); // Texto original (sem truncamento)

        // Truncar o texto para exibição
        String suggestionToDisplay = originalSuggestion;
        if (originalSuggestion.length() > 37) {
            suggestionToDisplay = originalSuggestion.substring(0, 37) + "..."; // Truncar e adicionar "..."
        }

        // Definir o texto com destaque
        holder.suggestionText.setText(getHighlightedText(suggestionToDisplay));

        // Passar o texto original (sem truncamento) no clique
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(originalSuggestion));
    }


    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView suggestionText;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestionText = itemView.findViewById(R.id.suggestion_text);
        }
    }

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    // Method to highlight the query in the suggestion text
    private CharSequence getHighlightedText(String suggestion) {
        if (query != null && !query.isEmpty()) {
            int startPos = suggestion.toLowerCase().indexOf(query.toLowerCase()); // Case-insensitive search
            if (startPos != -1) {
                int endPos = startPos + query.length();
                SpannableString spannableString = new SpannableString(suggestion);
                spannableString.setSpan(new ForegroundColorSpan(0xFF38B6FF), startPos, endPos, 0); // Blue color
                return spannableString;
            }
        }
        return suggestion; // Return the suggestion as-is if no match is found
    }
}
