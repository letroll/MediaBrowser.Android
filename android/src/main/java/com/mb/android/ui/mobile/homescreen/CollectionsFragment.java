package com.mb.android.ui.mobile.homescreen;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.mb.android.MainApplication;
import com.mb.android.R;
import com.mb.android.logging.AppLogger;
import com.mb.android.ui.mobile.library.LibraryPresentationActivity;
import com.mb.android.adapters.CollectionAdapter;
import mediabrowser.apiinteraction.Response;
import com.mb.android.interfaces.ICommandListener;
import com.mb.android.ui.mobile.music.MusicActivity;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.entities.SortOrder;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows a grid containing all the users media collections.
 */
public class CollectionsFragment extends Fragment implements ICommandListener {

    private BaseItemDto[] mItems;
    private ProgressBar mActivityIndicator;
    private GridView mCollectionsGrid;

    /**
     * Class Constructor
     */
    public CollectionsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_collection, container, false);

        if (rootView != null) {
            mActivityIndicator = (ProgressBar) rootView.findViewById(R.id.pbActivityIndicator);
            mCollectionsGrid = (GridView) rootView.findViewById(R.id.gvCollectionGrid);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (MainApplication.getInstance().API != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getCurrentUserId())) {

            ItemQuery query = new ItemQuery();
            query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
            query.setSortBy(new String[]{ItemSortBy.SortName});
            query.setSortOrder(SortOrder.Ascending);
            query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});

            // Retrieve the root items.
            MainApplication.getInstance().API.GetItemsAsync(query, getItemsResponse);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            mActivityIndicator.setVisibility(View.GONE);

            if (response != null) {
                try {
                    AppLogger.getLogger().Info("", "Root items received.");
                    mItems = response.getItems();

                    if (mCollectionsGrid == null) return;

                    mCollectionsGrid.setAdapter(new CollectionAdapter(mItems, MainApplication.getInstance(), MainApplication.getInstance().API));
                    mCollectionsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
                            if (mItems[index].getCollectionType() != null && mItems[index].getCollectionType().equalsIgnoreCase("music")) {
                                Intent intent = new Intent(MainApplication.getInstance(), MusicActivity.class);
                                intent.putExtra("ParentId", mItems[index].getId());
                                startActivity(intent);
                            } else {
                                String jsonData = MainApplication.getInstance().getJsonSerializer().SerializeToString(mItems[index]);
                                Intent intent = new Intent(MainApplication.getInstance(), LibraryPresentationActivity.class);
                                intent.putExtra("Item", jsonData);
                                startActivity(intent);
                            }
                        }
                    });
                } catch (Exception ex) {
                    AppLogger.getLogger().Info("", "Cannot cast data to ItemsResult");
                }

            } else {
                AppLogger.getLogger().Info("", "Invalid response when requesting Root Items");
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    @Override
    public void onPreviousButton() {

    }

    @Override
    public void onNextButton() {

    }

    @Override
    public void onPlayPauseButton() {

    }

    @Override
    public void onPlayButton() {

    }

    @Override
    public void onPauseButton() {

    }
}
