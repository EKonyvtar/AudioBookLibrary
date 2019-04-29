package com.murati.audiobook.ui;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.murati.audiobook.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder> {
    private List<MediaBrowserCompat.MediaItem> mediaItemList;
    private Context mContext;

    public RecyclerViewAdapter(Context context, List<MediaBrowserCompat.MediaItem> mediaItemList) {
        this.mediaItemList = mediaItemList;
        this.mContext = context;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_sidelist_ebook, null);
        //CustomViewHolder viewHolder = new CustomViewHolder(view);
        return null; //viewHolder;
    }

    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
        MediaBrowserCompat.MediaItem item = mediaItemList.get(i);

        //Setting text view title
        customViewHolder.titleView.setText("Hello");
        customViewHolder.descriptionView.setText("Bello");
    }

    @Override
    public int getItemCount() {
        return (null != mediaItemList ? mediaItemList.size() : 0);
    }

    class CustomViewHolder extends RecyclerView.ViewHolder {
        protected ImageView imageView;
        protected TextView titleView;
        protected TextView descriptionView;

        public CustomViewHolder(View view) {
            super(view);

            this.imageView = (ImageView) view.findViewById(R.id.play_eq);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.descriptionView = (TextView) view.findViewById(R.id.description);
        }
    }
}
