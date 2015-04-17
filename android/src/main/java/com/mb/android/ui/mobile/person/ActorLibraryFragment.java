package com.mb.android.ui.mobile.person;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.mb.android.MainApplication;
import com.mb.android.R;
import com.mb.android.activities.mobile.MediaDetailsActivity;
import com.mb.android.adapters.MediaAdapterBackdrops;
import com.mb.android.adapters.MediaAdapterPosters;
import com.mb.android.logging.AppLogger;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.entities.SortOrder;

import java.util.Arrays;

/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows a grid of media that the person has been involved in (actor, director, producer)
 */
public class ActorLibraryFragment extends Fragment {

    private GridView mActorFilmography;
    private boolean mPostersEnabled;
    private ActorBioActivity mActorBioActivity;

    /**
     * Class Constructor
     */
    public ActorLibraryFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActorBioActivity);
        mPostersEnabled = true;

        View mView;
        if (mPostersEnabled) {
            mView = inflater.inflate(R.layout.fragment_library_presentation_poster, container, false);
            mActorFilmography = (GridView) mView.findViewById(R.id.gvLibrary);
        } else {
            mView = inflater.inflate(R.layout.fragment_actor_library, container, false);
            mActorFilmography = (GridView) mView.findViewById(R.id.gvActorMediaList);
        }

        return mView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            try {
                mActorBioActivity = (ActorBioActivity) activity;
            } catch (ClassCastException e) {
                AppLogger.getLogger().Debug("ServerSelectionFragment", "onAttach: Exception casting activity");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();

        String actorId = args.getString("ActorId");

        // Get all the media in the users library containing this person
        ItemQuery query = new ItemQuery();
        query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
        query.setSortBy(new String[]{ItemSortBy.PremiereDate});
        query.setSortOrder(SortOrder.Descending);
        query.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
        query.setRecursive(true);
        query.setPersonIds(new String[]{actorId});

        MainApplication.getInstance().API.GetItemsAsync(query, getItemsResponse);
    }

    private Response<ItemsResult> getItemsResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(final ItemsResult response) {
            if (response != null && response.getItems() != null) {

                if (mPostersEnabled)
                    mActorFilmography.setAdapter(new MediaAdapterPosters(
                            Arrays.asList(response.getItems()),
                            MainApplication.getInstance().getResources().getInteger(R.integer.library_columns_poster),
                            MainApplication.getInstance().API,
                            R.drawable.default_video_portrait)
                    );
                else
                    mActorFilmography.setAdapter(new MediaAdapterBackdrops(mActorBioActivity, Arrays.asList(response.getItems()), MainApplication.getInstance().API, R.drawable.default_video_landscape));

                    mActorFilmography.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String jsonData = MainApplication.getInstance().getJsonSerializer().SerializeToString(response.getItems()[i]);

                        Intent intent = new Intent(mActorBioActivity, MediaDetailsActivity.class);
                        intent.putExtra("Item", jsonData);

                        startActivity(intent);
                    }
                });

                mActorFilmography.requestFocus();

            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActorBioActivity = null;
    }
}
