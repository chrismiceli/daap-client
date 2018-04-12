package org.mult.daap;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mult.daap.client.Playlist;

import java.util.ArrayList;

/**
 * The adapter that handles rendering the playlist items for the RecyclerListView
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private ArrayList<Playlist> playlists;

    private RecyclerOnItemClickListener<Playlist> onItemClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView playlistNameTextView;

        public ViewHolder(View v) {
            super(v);
            playlistNameTextView = v.findViewById(R.id.simple_row_text);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PlaylistAdapter(ArrayList<Playlist> playlists) {
        this.playlists = playlists;
    }

    @Override
    public PlaylistAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.simple_row_item, null);
        return new PlaylistAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Playlist listItem = this.playlists.get(position);
        holder.playlistNameTextView.setText(listItem.getName());
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
        return playlists.size();
    }

    public void setOnItemClickListener(RecyclerOnItemClickListener<Playlist> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}