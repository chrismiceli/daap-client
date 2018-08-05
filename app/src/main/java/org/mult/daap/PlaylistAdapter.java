package org.mult.daap;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mult.daap.db.entity.PlaylistEntity;

/**
 * The adapter that handles rendering the playlist items for the RecyclerListView
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private final PlaylistEntity[] playlists;

    private RecyclerOnItemClickListener<PlaylistEntity> onItemClickListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView playlistNameTextView;

        ViewHolder(View v) {
            super(v);
            playlistNameTextView = v.findViewById(R.id.simple_row_text);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    PlaylistAdapter(PlaylistEntity[] playlists) {
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
        final PlaylistEntity listItem = this.playlists[position];
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
        return playlists.length;
    }

    public void setOnItemClickListener(RecyclerOnItemClickListener<PlaylistEntity> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}