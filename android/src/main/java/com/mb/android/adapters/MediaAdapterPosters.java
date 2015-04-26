package com.mb.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MainApplication;
import com.mb.android.R;
import com.mb.android.utils.Utils;
import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.ItemLayout;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import com.mb.android.logging.AppLogger;

import java.util.Date;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Adapter that displays info from a DTOBaseItem
 */
public class MediaAdapterPosters extends BaseAdapter implements SectionIndexer {

    List<BaseItemDto> mBaseItems;
    LayoutInflater li;
    ApiClient mApi;
    SharedPreferences mSharedPreferences;
    double mImageWidth;
    double mImageHeight;
    Integer mDefaultImageId;
    private boolean imageEnhancersEnabled;
    private String sections_ = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public MediaAdapterPosters(List<BaseItemDto> baseItems, int columns, ApiClient apiClient, Integer defaultImageId) {
        mBaseItems = baseItems;
        mApi = apiClient;
        mDefaultImageId = defaultImageId;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance());
        try {
            li = (LayoutInflater) MainApplication.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            DisplayMetrics dm = MainApplication.getInstance().getResources().getDisplayMetrics();

            mImageWidth = dm.widthPixels / columns;

            Double displayAspectRatio = ItemLayout.GetDisplayAspectRatio(baseItems);

            if (displayAspectRatio != null && displayAspectRatio > 0) {
                mImageHeight = (mImageWidth / displayAspectRatio);
                AppLogger.getLogger().Debug("CAR","greater than 0");
            } else {
                mImageHeight = mImageWidth * 1.5;
            }
            AppLogger.getLogger().Debug("Height", String.valueOf(mImageHeight));
            AppLogger.getLogger().Debug("Width", String.valueOf(mImageWidth));

            imageEnhancersEnabled = PreferenceManager
                    .getDefaultSharedPreferences(MainApplication.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true);

        } catch (Exception e) {
            AppLogger.getLogger().ErrorException("Error in adapter initialization", e);
        }

    }


    public int getCount() {
        return mBaseItems.size();
    }


    @SuppressLint("SimpleDateFormat")
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = li.inflate(R.layout.widget_folder_button, parent, false);

            if (convertView == null) return null;

            holder = new ViewHolder();
            holder.titleText = (TextView) convertView.findViewById(R.id.tvFolderButtonTitle);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.tvFolderButtonYear);
            holder.imageView = (NetworkImageView) convertView.findViewById(R.id.ivFolderButtonImage);
            holder.isNewOverlay = (TextView) convertView.findViewById(R.id.tvOverlay);
            holder.missingEpisodeOverlay = (TextView) convertView.findViewById(R.id.tvMissingEpisodeOverlay);
            holder.playedProgress = (ProgressBar) convertView.findViewById(R.id.pbPlaybackProgress);

            holder.imageView.setLayoutParams(new RelativeLayout.LayoutParams((int)mImageWidth, (int)mImageHeight));

            if (mDefaultImageId != null) {
                holder.imageView.setDefaultImageResId(mDefaultImageId);
            }

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.position = position;

        BaseItemDto currentItem = mBaseItems.get(position);

        String type = currentItem.getType();

        // Set primary text

        if (type.equalsIgnoreCase("Episode") && currentItem.getIndexNumber() != null) {

            if (currentItem.getAirsBeforeSeasonNumber() != null ||
                    currentItem.getAirsAfterSeasonNumber() != null ||
                    currentItem.getAirsBeforeEpisodeNumber() != null) {

                holder.titleText.setText(currentItem.getName());

            } else {
                String title = currentItem.getIndexNumber().toString();
                if (currentItem.getIndexNumberEnd() != null && !currentItem.getIndexNumberEnd().equals(currentItem.getIndexNumber()))
                    title += " - " + currentItem.getIndexNumberEnd() + ". ";
                else
                    title += ". ";
                title += currentItem.getName();
                holder.titleText.setText(title);
            }
            holder.titleText.setVisibility(View.VISIBLE);
        }
        else if (type.equalsIgnoreCase("Series") || type.equalsIgnoreCase("Movie")){
            holder.titleText.setVisibility(View.GONE);
        }
        else {
            holder.titleText.setVisibility(View.VISIBLE);
            holder.titleText.setText(currentItem.getName());
        }

        // Set Secondary text

        if (type.equalsIgnoreCase("Boxset") || type.equalsIgnoreCase("Folder") || type.equalsIgnoreCase("Season") || type.equalsIgnoreCase("PhotoFolder") || type.equalsIgnoreCase("Series") || type.equalsIgnoreCase("Movie")) {

            holder.secondaryText.setVisibility(View.GONE);

        } else {

            if (currentItem.getProductionYear() != null && currentItem.getProductionYear() != 0) {
                holder.secondaryText.setVisibility(View.VISIBLE);
                holder.secondaryText.setText(String.valueOf(currentItem.getProductionYear()));
            }
        }

        // Set poster image
        String imageUrl = "";

        ImageOptions options = null;

        if (currentItem.getHasPrimaryImage()) {
            options = MainApplication.getInstance().getImageOptions(ImageType.Primary);
            options.setWidth((int)mImageWidth);
            Double aspectRatio = ItemLayout.GetDisplayAspectRatio(currentItem);
            if (aspectRatio != null && aspectRatio > 0){
                options.setHeight((int) (mImageWidth / aspectRatio));
            }
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(currentItem, options);

        } else if (currentItem.getType().equalsIgnoreCase("episode")
                && currentItem.getParentThumbItemId() != null) {
            options = MainApplication.getInstance().getImageOptions(ImageType.Thumb);
            options.setMaxWidth((int)mImageWidth);
            options.setEnableImageEnhancers(imageEnhancersEnabled);
            imageUrl = mApi.GetImageUrl(currentItem.getParentThumbItemId(), options);
        } else if (mDefaultImageId != null) {
            holder.imageView.setImageUrl(null, MainApplication.getInstance().API.getImageLoader());
        }

        if (options != null) {
            holder.imageView.setImageUrl(imageUrl, MainApplication.getInstance().API.getImageLoader());
        }

        // Process top-right overlays
        if (currentItem.getLocationType().equals(LocationType.Virtual) && currentItem.getType().equalsIgnoreCase("episode")) {
            holder.isNewOverlay.setVisibility(View.INVISIBLE);

            if (currentItem.getPremiereDate() != null) {

                Date premiereDate = Utils.convertToLocalDate(currentItem.getPremiereDate());

                long premiereDateMs = premiereDate.getTime();
                long currentMs = new Date().getTime();

                if (premiereDateMs - currentMs > 0)
                    holder.missingEpisodeOverlay.setText(MainApplication.getInstance().getResources().getString(R.string.un_aired_overlay));
            }

            holder.missingEpisodeOverlay.setVisibility(TextView.VISIBLE);
        } else {
            holder.missingEpisodeOverlay.setVisibility(TextView.GONE);
            if (currentItem.getType().equalsIgnoreCase("boxset") ||
                    currentItem.getType().equalsIgnoreCase("season") ||
                    currentItem.getType().equalsIgnoreCase("series")) {
                if (currentItem.getUserData().getUnplayedItemCount() != null && currentItem.getUserData().getUnplayedItemCount() > 0) {
                    holder.isNewOverlay.setText(String.valueOf(currentItem.getUserData().getUnplayedItemCount()));
                    holder.isNewOverlay.setVisibility(View.VISIBLE);
                } else if (currentItem.getUserData().getUnplayedItemCount() != null && currentItem.getUserData().getUnplayedItemCount() == 0) {
                    holder.isNewOverlay.setText("\u2714");
                    holder.isNewOverlay.setVisibility(View.VISIBLE);
                } else {
                    holder.isNewOverlay.setVisibility(View.GONE);
                }
            } else {
                new AsyncProcessOverlay(holder.isNewOverlay, currentItem).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }


        try {

            if (currentItem.getUserData() != null && currentItem.getUserData().getPlaybackPositionTicks() > 1) {

                holder.playedProgress.setVisibility(View.VISIBLE);
                holder.playedProgress.setMax(100);
                double percentWatched = (double)currentItem.getUserData().getPlaybackPositionTicks() / (double) currentItem.getRunTimeTicks();
                int roundedValue = (int) (percentWatched * 100);
                holder.playedProgress.setProgress(roundedValue);

            } else {
                holder.playedProgress.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            holder.playedProgress.setVisibility(View.INVISIBLE);
            AppLogger.getLogger().ErrorException("Error setting progressbar value", e);
        }

        return convertView;
    }


    public Object getItem(int position) {

        return mBaseItems.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }


    @SuppressLint("DefaultLocale")
    public int getPositionForSection(int section) {
        // If there is no item for current section, previous section will be selected
        for (int i = section; i >= 0; i--) {
            for (int j = 0; j < getCount(); j++) {
                if (i == 0) {
                    // For numeric section
                    for (int k = 0; k <= 9; k++) {
                        if (((BaseItemDto) getItem(j)).getSortName().charAt(0) == (char) k)
                            return j;
                    }
                } else {
                    if (((BaseItemDto) getItem(j)).getSortName().toUpperCase().charAt(0) == sections_.charAt(i))
                        return j;
                }
            }
        }
        return 0;
    }


    public int getSectionForPosition(int position) {
        return 0;
    }


    public Object[] getSections() {
        String[] sections = new String[sections_.length()];
        for (int i = 0; i < sections_.length(); i++)
            sections[i] = String.valueOf(sections_.charAt(i));
        return sections;
    }


    public class ViewHolder {

        public NetworkImageView imageView;
        public TextView titleText;
        public TextView secondaryText;
        public TextView isNewOverlay;
        public ProgressBar playedProgress;
        public TextView missingEpisodeOverlay;
        public int position;
    }


    public class AsyncProcessOverlay extends AsyncTask<Void, Void, String> {

        private TextView mTextView;
        private BaseItemDto mItem;

        public AsyncProcessOverlay(TextView textView, BaseItemDto item) {
            mTextView = textView;
            mItem = item;
        }

        @Override
        protected String doInBackground(Void... voids) {

            boolean isNew = false;

            if (mItem.getDateCreated() != null) {

                Date createdDate = Utils.convertToLocalDate(mItem.getDateCreated());

                long createdMilliseconds = createdDate.getTime();
                long currentMilliseconds = new Date().getTime();

                if ((currentMilliseconds - createdMilliseconds) < (10 * 86400000)) {
                    isNew = true;
                }
            }

            if (mItem.getUserData() != null) {

                if (mItem.getUserData().getPlayed()) {
                    return "watched";
                } else {
                    return "unwatched";
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String string) {


            if (string.equalsIgnoreCase("watched")) {

                mTextView.setText("\u2714");
                mTextView.setVisibility(View.VISIBLE);

            } else {

                mTextView.setVisibility(View.INVISIBLE);
            }

        }
    }

}
