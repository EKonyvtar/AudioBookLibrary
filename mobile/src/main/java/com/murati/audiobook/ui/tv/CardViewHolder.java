/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.murati.audiobook.ui.tv;

import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_GENRE;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_WRITER;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import android.support.v4.media.MediaDescriptionCompat;
import android.view.View;

import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;
import com.murati.audiobook.ui.MediaItemViewHolder;
import com.murati.audiobook.utils.BitmapHelper;

public class CardViewHolder extends Presenter.ViewHolder {

    private static final int CARD_WIDTH = 192; //300;
    private static final int CARD_HEIGHT = 236; //250;

    private final ImageCardView mCardView;
    private int mItemState;

    public CardViewHolder(View view) {
        super(view);
        mCardView = (ImageCardView) view;
        mItemState = MediaItemViewHolder.STATE_NONE;
    }

    public void setState(int state) {
        mItemState = state;
    }

    public void attachView() {
        if (mItemState == MediaItemViewHolder.STATE_PLAYING) {
            AnimationDrawable badgeDrawable = (AnimationDrawable) mCardView.getBadgeImage();
            if (badgeDrawable != null) {
                badgeDrawable.start();
            }
        }
    }

    public void detachView() {
        if (mItemState == MediaItemViewHolder.STATE_PLAYING) {
            AnimationDrawable badgeDrawable = (AnimationDrawable) mCardView.getBadgeImage();
            if (badgeDrawable != null) {
                badgeDrawable.stop();
            }
        }
    }

    public void setBadgeImage(Drawable drawable) {
        mCardView.setBadgeImage(drawable);
    }

    /**
     * Set the view in this holder to represent the media metadata in {@code description}
     *
     **/
    public void setupCardView(final Context context, MediaDescriptionCompat description) {

        String title = String.valueOf(description.getTitle());
        // Hack for shorter titles for Bible TV
        if ("bible_hu".equals(BuildConfig.FLAVOR_catalogue)) {
            if (title.contains("-"))
                title = title.split("-")[1];
        }

        mCardView.setTitleText(title);
        mCardView.setContentText(description.getSubtitle());
        mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

        // Based on state of item, set or unset badge
        Drawable drawable = MediaItemViewHolder.getDrawableByState(context, mItemState);
        mCardView.setBadgeImage(drawable);


        // Set Bitmap to default
        setCardImage(context, description.getIconBitmap());

        // The try to load extra if present
        if (description.getIconUri() != null) {
            //TODO: readjust scaling for TV
            BitmapHelper.fetch(context, description.getIconUri().toString(), new BitmapHelper.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    setCardImage(context, bitmap);
                }
            });
        } else {
            if (description.getMediaId().startsWith(MEDIA_ID_BY_GENRE))
                setCardImage(context, description.getIconBitmap());
            else if (description.getMediaId().startsWith(MEDIA_ID_BY_WRITER))
                setCardImage(context, description.getIconBitmap());
        }
    }

    private void setCardImage(Context context, Bitmap art) {
        if (mCardView == null) {
            return;
        }
        Drawable artDrawable = null;

        if (art != null) {
            artDrawable = new BitmapDrawable(context.getResources(), art);
        } else {
            artDrawable = context.getDrawable(R.drawable.default_book_cover);

            /* CharSequence title = mCardView.getTitleText();
            if (title != null && title.length() > 0) {
                String text = String.valueOf(title);
                //text = text.replace(" ", "\n");
                artDrawable = new TextDrawable(text);
            } */
        }
        mCardView.setMainImage(artDrawable);
    }

    /**
     * Simple drawable that draws a text (letter, in this case). Used with the media title when
     * the MediaDescription has no corresponding album art.
     */
    private static class TextDrawable extends Drawable {

        private final String text;
        private final Paint paint;

        public TextDrawable(String text) {
            this.text = text;
            this.paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(20f);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(6f, 0, 0, Color.BLACK);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect r = getBounds();
            int count = canvas.save();
            canvas.translate(r.left, r.top);
            //float midW = r.width() / 2;
            //float midH = r.height() / 2 - ((paint.descent() + paint.ascent()) / 2);
            //canvas.drawText(text, midW, midH, paint);
            canvas.drawText(text, 30, 170, paint);
            canvas.restoreToCount(count);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
