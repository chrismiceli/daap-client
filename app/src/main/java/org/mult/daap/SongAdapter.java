package org.mult.daap;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

/**
 * The adapter that handles rendering the songs items for the RecyclerListView
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private final List<SongEntity> songs;

    private RecyclerOnItemClickListener<SongEntity> onItemClickListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView playlistNameTextView;

        ViewHolder(View v) {
            super(v);
            playlistNameTextView = v.findViewById(R.id.simple_row_text);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    SongAdapter(List<SongEntity> songs) {
        this.songs = songs;
    }

    @Override
    public SongAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.simple_row_item, null);
        return new SongAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SongEntity listItem = this.songs.get(position);
        holder.playlistNameTextView.setText(listItem.name);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(listItem);
            }
        };

        holder.playlistNameTextView.setOnClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void setOnItemClickListener(RecyclerOnItemClickListener<SongEntity> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}