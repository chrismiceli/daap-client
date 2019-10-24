package org.mult.daap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * The adapter that handles rendering the albums and artists items for the RecyclerListView
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private final List<String> items;

    private RecyclerOnItemClickListener<String> onItemClickListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        ViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.simple_row_text);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    ItemAdapter(List<String> items) {
        this.items = items;
    }

    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.simple_row_item, null);
        return new ItemAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final String listItem = this.items.get(position);
        holder.textView.setText(listItem);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(listItem);
            }
        };

        holder.textView.setOnClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setOnItemClickListener(RecyclerOnItemClickListener<String> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}