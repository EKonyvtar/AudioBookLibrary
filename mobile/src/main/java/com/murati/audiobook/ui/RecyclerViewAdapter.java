package com.murati.audiobook.ui;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.murati.audiobook.R;
import com.murati.audiobook.utils.LogHelper;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder> {
    private static final String TAG = LogHelper.makeLogTag(RecyclerViewAdapter.class);

    private List<MediaBrowserCompat.MediaItem> mediaItemList;
    private Context mContext;

    public RecyclerViewAdapter(Context context, List<MediaBrowserCompat.MediaItem> mediaItemList) {
        this.mediaItemList = mediaItemList;
        this.mContext = context;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.
            from(viewGroup.getContext()).
            inflate(R.layout.fragment_sidelist_ebook, viewGroup, false); //viewGroup,true);
        CustomViewHolder viewHolder = new CustomViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
        //Setting text view title
        try {

            MediaBrowserCompat.MediaItem item = mediaItemList.get(i);

            customViewHolder.titleView.setText(item.getDescription().getTitle());
            customViewHolder.descriptionView.setText(item.getDescription().getSubtitle());

            //Load image
            Uri imageUri = item.getDescription().getIconUri();
            GlideApp.
                with(customViewHolder.itemView).
                load(imageUri).
                //centerCrop().
                fallback(R.drawable.default_book_cover).
                error(R.drawable.default_book_cover).
                    into(customViewHolder.imageView);
        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }
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

            try {
                this.imageView = (ImageView) view.findViewById(R.id.play_eq);
                this.titleView = (TextView) view.findViewById(R.id.title);
                this.descriptionView = (TextView) view.findViewById(R.id.description);
            } catch (Exception ex) {
                Log.e(TAG,ex.getMessage());
            }
        }
    }
}
