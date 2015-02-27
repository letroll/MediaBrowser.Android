package com.mb.android.ui.mobile.person;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MainApplication;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import com.mb.android.logging.AppLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;


/**
 * Created by Mark on 12/12/13.
 *
 * Fragment that shows various details about a person
 */
public class ActorBioFragment extends Fragment {

    private static final String TAG = "ActorBioFragment";
    private TextView mActorBio;
    private String mActorId;
    private NetworkImageView mActorImage;

    /**
     * Class Constructor
     */
    public ActorBioFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        AppLogger.getLogger().Info(TAG + ": start Creating view");
        View mView = inflater.inflate(R.layout.fragment_actor_bio, container, false);

        Bundle args = getArguments();
        mActorId = args.getString("ActorId");
        mActorBio = (TextView) mView.findViewById(R.id.tvActorBio);
        mActorImage = (NetworkImageView) mView.findViewById(R.id.ivActorImageLarge);

        AppLogger.getLogger().Info(TAG + ": finished creating view");
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();

        AppLogger.getLogger().Info(TAG + ": Requesting person details");

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(mActorId)) {

            MainApplication.getInstance().API.GetItemAsync(mActorId, MainApplication.getInstance().API.getCurrentUserId(), getPersonResponse);
            int actorImageMaxHeight = (int) (360 * 1.5);
            int actorImageMaxWidth = (int) (360 * 1.5);
            AppLogger.getLogger().Info(TAG + ": build actor image query");
            // Get the Actor image
            ImageOptions actorImageOptions = new ImageOptions();
            actorImageOptions.setMaxHeight(actorImageMaxHeight);
            actorImageOptions.setMaxWidth(actorImageMaxWidth);
            actorImageOptions.setImageType(ImageType.Primary);
            actorImageOptions.setEnableImageEnhancers(PreferenceManager
                    .getDefaultSharedPreferences(MainApplication.getInstance())
                    .getBoolean("pref_enable_image_enhancers", true));

            String imageUrl = MainApplication.getInstance().API.GetImageUrl(mActorId, actorImageOptions);
            mActorImage.setImageUrl(imageUrl, MainApplication.getInstance().API.getImageLoader());
            AppLogger.getLogger().Info(TAG + ": image query sent");
        }
    }

    private Response<BaseItemDto> getPersonResponse = new Response<BaseItemDto>() {

        @Override
        public void onResponse(BaseItemDto actor) {
            AppLogger.getLogger().Info(TAG + ": Actor bio response");
            if (actor != null) {
                mActorBio.setText(actor.getOverview());
                mActorBio.setMovementMethod(new ScrollingMovementMethod());
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
