package com.mb.android.ui.tv.library;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MainApplication;
import com.mb.android.R;
import com.mb.android.displaypreferences.DisplayPreference;
import com.mb.android.displaypreferences.ViewType;
import com.mb.android.logging.AppLogger;
import com.mb.android.ui.tv.library.dialogs.LongPressDialogFragment;
import com.mb.android.ui.tv.library.dialogs.MediaResumeDialogFragment;
import com.mb.android.ui.tv.library.dialogs.MenuDialogFragment;
import com.mb.android.ui.tv.library.dialogs.QuickPlayDialogFragment;
import com.mb.android.ui.tv.library.interfaces.ILongPressDialogListener;
import com.mb.android.ui.tv.library.interfaces.IQuickPlayDialogListener;
import com.mb.android.ui.tv.library.interfaces.IViewChangeListener;
import mediabrowser.apiinteraction.Response;

import com.mb.android.ui.tv.MbBackdropActivity;
import com.mb.android.ui.tv.playback.PlayerHelpers;
import mediabrowser.model.channels.ChannelItemQuery;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ArtistsQuery;
import mediabrowser.model.querying.EpisodeQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows a grid containing all the children of the current item
 */
public class LibraryActivity
        extends MbBackdropActivity
        implements ILongPressDialogListener, IQuickPlayDialogListener, IViewChangeListener {

    private static final String TAG = "LibraryActivity";
    private BaseLibraryFragment libraryFragment;
    private List<BaseItemDto> mItems;
    private BaseItemDto mParent;
    private ItemQuery mQuery;
    private ArtistsQuery mArtistsQuery;
    private String mParentCollectionType;
    private ProgressBar mActivityIndicator;
    private TextView mErrorText;
    private ImageView overlay;
    private int mCurrentQueryStartIndex = 0;
    private boolean mIsFresh = true;
    private PlayerHelpers mPlayHelper;
    private DisplayPreference displayPreference;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        AppLogger.getLogger().Info(TAG + ": Create Activity");
        setContentView(R.layout.tv_activity_library);
        mBackdropSwitcher = (ViewSwitcher) findViewById(R.id.vsBackdropImages);
        mBackdropImage1 = (NetworkImageView) findViewById(R.id.ivBackdropImage1);
        mBackdropImage2 = (NetworkImageView) findViewById(R.id.ivBackdropImage2);
        overlay = (ImageView) findViewById(R.id.ivBackdropOverlay);
        mActivityIndicator = (ProgressBar) findViewById(R.id.pbActivityIndicator);
        mErrorText = (TextView) findViewById(R.id.tvErrorText);
        String jsonData = getIntent().getStringExtra("CurrentBaseItemDTO");
        mParent = MainApplication.getInstance().getJsonSerializer().DeserializeFromString(jsonData, BaseItemDto.class);
        mParentCollectionType = getIntent().getStringExtra("CollectionType");
        setOverscanValues();
        AppLogger.getLogger().Info(TAG + ": Finished creating Activity");
        mPlayHelper = new PlayerHelpers();
        onGridSelected();
        mItems = new ArrayList<>();

        displayPreference = MainApplication.getInstance().getPreferenceManager().retrieveDisplayPreference(mParent);
        setInitialView();
    }

    private void setInitialView() {

        if (displayPreference == null || displayPreference.viewType == ViewType.LIBRARY_GRID) {
            onGridSelected();
        } else if (displayPreference.viewType == ViewType.LIBRARY_COVERFLOW) {
            onCoverflowSelected();
        } else if (displayPreference.viewType == ViewType.LIBRARY_LIST) {
            onListSelected();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppLogger.getLogger().Info("CollectionItemsFragment", "onDestroy");
    }


    @Override
    public void onResume() {
        super.onResume();
        AppLogger.getLogger().Info(TAG + ": resuming activity");
        buildAndSendQuery();
    }

    @Override
    public void onPause() {
        super.onPause();
        MainApplication.getInstance().getPreferenceManager().saveDisplayPreference(mParent, displayPreference);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (libraryFragment != null) {
                    if (libraryFragment.onDpadLeftHandled()) return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (libraryFragment != null) {
                    if (libraryFragment.onDpadRightHandled()) return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (libraryFragment != null) {
                    if (libraryFragment.onDpadUpHandled()) return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (libraryFragment != null) {
                    if (libraryFragment.onDpadDownHandled()) return true;
                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConnectionRestored() {
        AppLogger.getLogger().Info(TAG + ": connection restored");
        buildAndSendQuery();
    }

    @Override
    protected void onUserDataUpdated(String itemId, UserItemDataDto userItemDataDto) {
        AppLogger.getLogger().Debug("onUserDataUpdated");
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(itemId) || userItemDataDto == null) {
            return;
        }
        AppLogger.getLogger().Debug("Parsing dataset");
        for (BaseItemDto item : mItems) {
            if (itemId.equalsIgnoreCase(item.getId())) {
                AppLogger.getLogger().Debug("item matched");
                item.setUserData(userItemDataDto);
                if (libraryFragment != null) {
                    AppLogger.getLogger().Debug("Notify fragment of change");
                    libraryFragment.refreshData(item);
                }
                break;
            }
        }
        setResult(Activity.RESULT_OK);
    }

    private void buildAndSendQuery() {
        AppLogger.getLogger().Info(TAG + ": preparing to build query");
        if (mIsFresh & mParent != null) {
            if ("series".equalsIgnoreCase(mParent.getType())) {
                buildEpisodeQuery();
            } else if ("channel".equalsIgnoreCase(mParent.getType()) || "channelfolderitem".equalsIgnoreCase(mParent.getType())) {
                buildChannelQuery();
            } else if ("music".equalsIgnoreCase(mParent.getCollectionType())) {
                buildArtistQuery();
            } else {
                buildStandardItemQuery();
                if ("musicartist".equalsIgnoreCase(mParent.getType()) || "musicalbum".equalsIgnoreCase(mParent.getType())) {
                    populateBackdrops(mParent);
                }
            }
            mIsFresh = false;
        }
    }

    private void buildEpisodeQuery() {
        AppLogger.getLogger().Info(TAG + ": building episode query");
        String seasonId = getIntent().getStringExtra("SeasonId");
        EpisodeQuery episodeQuery = new EpisodeQuery();
        episodeQuery.setUserId(MainApplication.getInstance().API.getCurrentUserId());
        episodeQuery.setSeriesId(mParent.getId());
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(seasonId)) {
            episodeQuery.setSeasonId(String.valueOf(seasonId));
        }
        episodeQuery.setFields(new ItemFields[]{ItemFields.SortName, ItemFields.PrimaryImageAspectRatio});
        MainApplication.getInstance().API.GetEpisodesAsync(episodeQuery, getItemsResponse);
        populateBackdrops(mParent);
    }

    private void buildChannelQuery() {
        ChannelItemQuery query = new ChannelItemQuery();
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mParent.getChannelId())) {
            query.setChannelId(mParent.getChannelId());
            query.setFolderId(mParent.getId());
        } else {
            query.setChannelId(mParent.getId());
        }
        query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
        query.setSortOrder(SortOrder.Ascending);
        MainApplication.getInstance().API.GetChannelItems(query, getItemsResponse);
    }

    private void buildArtistQuery() {
        mArtistsQuery = new ArtistsQuery();
        mArtistsQuery.setParentId(mParent.getId());
        mArtistsQuery.setUserId(MainApplication.getInstance().API.getCurrentUserId());
        mArtistsQuery.setRecursive(true);
        mArtistsQuery.setSortBy(new String[]{ItemSortBy.SortName});
        mArtistsQuery.setSortOrder(SortOrder.Ascending);
        mArtistsQuery.setFields(new ItemFields[]{ItemFields.ParentId, ItemFields.PrimaryImageAspectRatio, ItemFields.SortName, ItemFields.Overview});
        mArtistsQuery.setStartIndex(0);
        mArtistsQuery.setLimit(200);

        MainApplication.getInstance().API.GetAlbumArtistsAsync(mArtistsQuery, getItemsResponse);
    }


    private void buildStandardItemQuery() {
        AppLogger.getLogger().Info(TAG + ": building item query");
        mQuery = new ItemQuery();
        mQuery.setUserId(MainApplication.getInstance().API.getCurrentUserId());
        mQuery.setParentId(mParent.getId());
        mQuery.setSortBy(new String[]{ItemSortBy.SortName});
        mQuery.setSortOrder(SortOrder.Ascending);
        mQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Overview, ItemFields.SortName, ItemFields.DateCreated, ItemFields.Genres,  ItemFields.CumulativeRunTimeTicks, ItemFields.Metascore});
        mQuery.setLimit(200);
        if ("movies".equalsIgnoreCase(mParent.getCollectionType())) {
            mQuery.setCollapseBoxSetItems(true);
            mQuery.setIncludeItemTypes(new String[] { "movie" });
            mQuery.setRecursive(true);
        } else if ("tvshows".equalsIgnoreCase(mParent.getCollectionType())) {
            mQuery.setIncludeItemTypes(new String[] { "series" });
            mQuery.setRecursive(true);
        }
        MainApplication.getInstance().API.GetItemsAsync(mQuery, getItemsResponse);
    }


    public void populateBackdrops(BaseItemDto item) {
        if ("episode".equalsIgnoreCase(item.getType()) || item.getBackdropCount() == 0) {
            return;
        }

        List<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < item.getBackdropCount(); i++) {

            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Backdrop);
            options.setImageIndex(i);
            options.setMaxHeight(720);

            String imageUrl = MainApplication.getInstance().API.GetImageUrl(item.getId(), options);
            imageUrls.add(imageUrl);
        }

        setBackdropImages(imageUrls);
    }


    @Override
    protected void onPlayButton() {

        if (libraryFragment == null) return;

        BaseItemDto currentItem;
        currentItem = libraryFragment.getCurrentItem();

        if (currentItem == null) return;


        if ("books".equalsIgnoreCase(mParentCollectionType) || "games".equalsIgnoreCase(mParentCollectionType) ) {
            showNotSupportedWarning();
        } else if (currentItem.getIsFolder()) {
            if (PlayerHelpers.isCollectionPlayableAsAudio(mParentCollectionType) || PlayerHelpers.isItemPlayableAsAudio(currentItem)) {
                showQuickPlayDialog(currentItem, true);
            } else if (PlayerHelpers.isCollectionPlayableAsVideo(mParentCollectionType) || PlayerHelpers.isItemPlayableAsVideo(currentItem)) {
                showQuickPlayDialog(currentItem, false);
            }
        } else {
            if (!"audio".equalsIgnoreCase(currentItem.getMediaType()) && !"video".equalsIgnoreCase(currentItem.getMediaType())) {
                showNotSupportedWarning();
            } else if (currentItem.getUserData() != null && currentItem.getUserData().getPlaybackPositionTicks() > 0) {
                showResumeDialog(currentItem);
            } else {
                mPlayHelper.playItem(this, currentItem, 0L, null, null, null, false);
            }
        }
    }


    private void showNotSupportedWarning() {
        Toast.makeText(this, "No quick play options available for this content type", Toast.LENGTH_LONG).show();
    }

    private void showQuickPlayDialog(BaseItemDto item, boolean isAudio) {

        QuickPlayDialogFragment quickPlayDialogFragment = new QuickPlayDialogFragment();
        quickPlayDialogFragment.setData(item, isAudio);
        quickPlayDialogFragment.show(getSupportFragmentManager(), "QuickPlayDialog");
    }

    private void showResumeDialog(BaseItemDto item) {

        MediaResumeDialogFragment resumeDialog = new MediaResumeDialogFragment();
        resumeDialog.setItem(item);
        resumeDialog.show(getSupportFragmentManager(), "resumeDialog");
    }

    public void showLongPressDialog(BaseItemDto item) {

        LongPressDialogFragment itemOptionsFragment = new LongPressDialogFragment();
        itemOptionsFragment.setItem(item);
        itemOptionsFragment.show(getSupportFragmentManager(), "OptionsFragment");
    }


    @Override
    protected void onFastForwardButton() {

//        AppLogger.getLogger().Debug("CollectionItemsFragment", "onRightShift");
//        if (mItemsGrid != null) {
//            AppLogger.getLogger().Debug("CollectionItemsFragment", "mItemsGrid != null");
//            AppLogger.getLogger().Debug("CollectionItemsFragment", "SelectedIndex = " + String.valueOf(mItemsGrid.getSelectedItemPosition()));
//            if (mItemsGrid.getSelectedItemPosition() + 12 < mItemsGrid.getCount()) {
//                mItemsGrid.setSelection(mItemsGrid.getSelectedItemPosition() + 12);
//                AppLogger.getLogger().Debug("CollectionItemsFragment", "smoothScrollToPosition");
//            } else {
//                mItemsGrid.smoothScrollToPosition(mItemsGrid.getCount() - 1);
//                AppLogger.getLogger().Debug("CollectionItemsFragment", "smoothScrollToPosition");
//            }
//        }
    }


    @Override
    protected void onRewindButton() {

//        AppLogger.getLogger().Debug("CollectionItemsFragment", "onLeftShift");
//        if (mItemsGrid != null) {
//            AppLogger.getLogger().Debug("CollectionItemsFragment", "mItemsGrid != null");
//            AppLogger.getLogger().Debug("CollectionItemsFragment", "SelectedIndex = " + String.valueOf(mItemsGrid.getSelectedItemPosition()));
//            if (mItemsGrid.getSelectedItemPosition() - 12 >= 0) {
//                mItemsGrid.setSelection(mItemsGrid.getSelectedItemPosition() - 12);
//                AppLogger.getLogger().Debug("CollectionItemsFragment", "smoothScrollToPosition");
//            } else {
//                mItemsGrid.smoothScrollToPosition(0);
//                AppLogger.getLogger().Debug("CollectionItemsFragment", "smoothScrollToPosition");
//            }
//        }
    }

    @Override
    protected void onMenuButton() {

        String currentView = "Grid";
        if (libraryFragment != null) {
            if (libraryFragment instanceof CoverFlowFragment) {
                currentView = "Cover Flow";
            } else if (libraryFragment instanceof ListFragment) {
                currentView = "List";
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString("CurrentView", currentView);
        MenuDialogFragment fragment = new MenuDialogFragment();
        fragment.setArguments(bundle);
        fragment.show(getSupportFragmentManager(), "MenuDialogFragment");
    }


    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            hideActivityIndicator();
            AppLogger.getLogger().Info(TAG + ": item response");
            if (response != null && response.getItems() != null && response.getItems().length > 0) {
                AppLogger.getLogger().Info(TAG + ": response contains items");
                mItems.addAll(Arrays.asList(response.getItems()));
                if (libraryFragment != null) {
                    libraryFragment.addContent(response.getItems());
                }
                mCurrentQueryStartIndex += response.getItems().length;

                if (response.getTotalRecordCount() > mCurrentQueryStartIndex + 1) {
                    AppLogger.getLogger().Info(TAG + ": more items to retrieve");
                    if (mArtistsQuery != null) {
                        mArtistsQuery.setStartIndex(mCurrentQueryStartIndex);
                        MainApplication.getInstance().API.GetAlbumArtistsAsync(mArtistsQuery, this);
                    } else {
                        mQuery.setStartIndex(mCurrentQueryStartIndex);
                        MainApplication.getInstance().API.GetItemsAsync(mQuery, this);
                    }
                }
            }
        }
        @Override
        public void onError(Exception ex) {
            AppLogger.getLogger().Info(TAG + ": item response error");
            hideActivityIndicator();
            // Only show an on-screen warning if no content has been shown yet
//            if (mItemsGrid.getAdapter() == null) {
                mErrorText.setVisibility(View.VISIBLE);
//            }
        }
    };


    private void hideActivityIndicator() {
        mActivityIndicator.setVisibility(View.GONE);
    }


    //******************************************************************************************************************
    // various dialog callbacks
    //******************************************************************************************************************

    @Override
    public void onUserDataChanged(String itemId, UserItemDataDto userItemDataDto) {
        onUserDataUpdated(itemId, userItemDataDto);
    }

    @Override
    public void onQuickPlaySelectionFinished() {
        mPlayHelper.playItems(this);
    }

    //******************************************************************************************************************
    // View Change methods
    //******************************************************************************************************************

    @Override
    public void onCoverflowSelected() {
        try {
            libraryFragment = new CoverFlowFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.flMainContainer, libraryFragment).commit();
            enableVignette();
            if (mItems != null && mItems.size() > 0) {
                libraryFragment.addContent(mItems.toArray(new BaseItemDto[mItems.size()]));
            }
            if (displayPreference != null) {
                displayPreference.viewType = ViewType.LIBRARY_COVERFLOW;
            }
        } catch (IllegalStateException ex) {
            Toast.makeText(this, "Unable to switch layouts", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onListSelected() {
        try {
            libraryFragment = new ListFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.flMainContainer, libraryFragment).commit();
            enableVignette();
            ((ListFragment)libraryFragment).setParent(mParent);
            if (mItems != null && mItems.size() > 0) {
                libraryFragment.addContent(mItems.toArray(new BaseItemDto[mItems.size()]));
            }
            if (displayPreference != null) {
                displayPreference.viewType = ViewType.LIBRARY_LIST;
            }
        } catch (IllegalStateException ex) {
            Toast.makeText(this, "Unable to switch layouts", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onGridSelected() {
        try {
            libraryFragment = new GridFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.flMainContainer, libraryFragment).commit();
            disableVignette();
            if (mItems != null && mItems.size() > 0) {
                libraryFragment.addContent(mItems.toArray(new BaseItemDto[mItems.size()]));
            }
            if (displayPreference != null) {
                displayPreference.viewType = ViewType.LIBRARY_GRID;
            }
        } catch (IllegalStateException ex) {
            Toast.makeText(this, "Unable to switch layouts", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onThumbSelected() {
//        try {
//            libraryFragment = new CoverflowFragment();
//            getSupportFragmentManager().beginTransaction().replace(R.id.flMainContainer, libraryFragment).commit();
//        } catch (IllegalStateException ex) {
//            Toast.makeText(this, "Unable to switch layouts", Toast.LENGTH_LONG).show();
//        }
    }

    @Override
    public void onStripSelected() {
//        try {
//            libraryFragment = new CoverflowFragment();
//            getSupportFragmentManager().beginTransaction().replace(R.id.flMainContainer, libraryFragment).commit();
//        } catch (IllegalStateException ex) {
//            Toast.makeText(this, "Unable to switch layouts", Toast.LENGTH_LONG).show();
//        }
    }

    private void enableVignette() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        ViewGroup.LayoutParams layoutParams = mBackdropSwitcher.getLayoutParams();
        layoutParams.height = (int)((double)metrics.heightPixels * .8);
        layoutParams.width = (int)((double)metrics.widthPixels * .8);
        mBackdropSwitcher.setLayoutParams(layoutParams);
        overlay.setImageResource(R.drawable.vignette_gradient);
    }

    private void disableVignette() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        ViewGroup.LayoutParams layoutParams = mBackdropSwitcher.getLayoutParams();
        layoutParams.height = metrics.heightPixels;
        layoutParams.width = metrics.widthPixels;
        mBackdropSwitcher.setLayoutParams(layoutParams);
        overlay.setImageResource(R.drawable.tv_gradient_left3);
    }
}
