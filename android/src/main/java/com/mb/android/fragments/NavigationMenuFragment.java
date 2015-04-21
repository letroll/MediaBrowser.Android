package com.mb.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mb.android.MainApplication;
import com.mb.android.MenuEntity;
import com.mb.android.Playlist;
import com.mb.android.R;
import com.mb.android.activities.mobile.ChannelsActivity;
import com.mb.android.activities.mobile.UserPreferencesActivity;
import com.mb.android.logging.AppLogger;
import com.mb.android.ui.mobile.homescreen.HomescreenActivity;
import com.mb.android.ui.mobile.library.LibraryPresentationActivity;
import com.mb.android.activities.mobile.PlaylistActivity;
import com.mb.android.adapters.ViewsAdapter;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;

import com.mb.android.player.AudioService;
import com.mb.android.ui.mobile.livetv.LiveTvActivity;
import com.mb.android.ui.main.ConnectionActivity;
import com.mb.android.ui.mobile.music.MusicActivity;
import mediabrowser.model.channels.ChannelQuery;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.LiveTvInfo;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-08-09.
 *
 * Fragment that shows the navigation menu on every activity when the user presses the app icon
 */
public class NavigationMenuFragment extends Fragment {

    private static final String TAG = "Navigation Menu";
    private ListView mDrawerList;
    private NetworkImageView mAvatar;
    private TextView mUsername;
    private RelativeLayout mUserContainer;
    private DrawerLayout mDrawerLayout;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_navigation_menu, container, false);
        mDrawerList = (ListView) view.findViewById(R.id.lvMainMenu);
        mAvatar = (NetworkImageView) view.findViewById(R.id.ivUserAvatar);
        mUsername = (TextView) view.findViewById(R.id.tvUserName);
        mUserContainer = (RelativeLayout) view.findViewById(R.id.rlUserContainer);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        // Don't start making api requests unless we have valid connection details
        if (ApiProperlyConfigured()) {
            if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().user.getName())) {
                MainApplication.getInstance().API.GetUserAsync(MainApplication.getInstance().API.getCurrentUserId(), getUserResponse);
            } else {
                buildUserButton();
            }
            getLibraryRoot();
        }
    }


    public void setDrawerLayout(DrawerLayout drawerLayout) {
        mDrawerLayout = drawerLayout;
    }


    private boolean ApiProperlyConfigured() {
        return MainApplication.getInstance().API != null
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getServerAddress())
                && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getCurrentUserId());
    }


    private Response<UserDto> getUserResponse = new Response<UserDto>() {
        @Override
        public void onResponse(UserDto result) {
            if (result == null) return;
            MainApplication.getInstance().user = result;
            buildUserButton();
        }
    };


    private void buildUserButton() {

        DisplayMetrics metrics = MainApplication.getInstance().getResources().getDisplayMetrics();

        if (MainApplication.getInstance().user != null) {
            if (MainApplication.getInstance().user.getHasPrimaryImage()) {

                ImageOptions options = new ImageOptions();
                options.setImageType(ImageType.Primary);
                options.setHeight((int) (56 * metrics.density));
                options.setWidth((int) (56 * metrics.density));
                options.setCropWhitespace(true);

                String imageUrl = MainApplication.getInstance().API.GetUserImageUrl(MainApplication.getInstance().user, options);
                mAvatar.setImageUrl(imageUrl, MainApplication.getInstance().API.getImageLoader());
            }

            mUsername.setText(MainApplication.getInstance().user.getName());
        }

        mUserContainer.setOnClickListener(onUserClick);
    }


    private View.OnClickListener onUserClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (MainApplication.getInstance().user.getPolicy().getEnableUserPreferenceAccess()) {
                // closeDrawer();
                //Intent intent = new Intent(MainApplication.getInstance(), UserPreferencesActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                //startActivity(intent);
            }
        }
    };


    private void closeDrawer() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }
    }

    private void terminateQueuedMedia() {
        MainApplication.getInstance().PlayerQueue = new Playlist();
        AudioService.PlayerState currentState = MainApplication.getAudioService().getPlayerState();
        if (currentState.equals(AudioService.PlayerState.PLAYING) || currentState.equals(AudioService.PlayerState.PAUSED)) {
            MainApplication.getAudioService().stopMedia();
        }
    }


    private void showUserSelection() {
        Intent intent = new Intent(MainApplication.getInstance(), ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void buildNavigationMenu(BaseItemDto[] rootFolders) {

        final List<MenuEntity> menu = new ArrayList<>();

        MenuEntity homeMenuItem = new MenuEntity();
        homeMenuItem.CollectionType = "root";
        homeMenuItem.Name = MainApplication.getInstance().getResources().getString(R.string.menu_home);

        menu.add(homeMenuItem);

        if (queueHasItems()) {
            MenuEntity queueMenuItem = new MenuEntity();
            queueMenuItem.CollectionType = "queue";
            queueMenuItem.Name = MainApplication.getInstance().getResources().getString(R.string.queue_string);
            menu.add(queueMenuItem);
        }

        if (rootFolders != null) {
            for (BaseItemDto item : rootFolders) {

                MenuEntity menuItem = new MenuEntity();
                menuItem.CollectionType = item.getCollectionType();
                menuItem.Id = item.getId();
                menuItem.Name = item.getName();

                menu.add(menuItem);
            }
        }

        MenuEntity logoutMenuItem = new MenuEntity();
        logoutMenuItem.CollectionType = "logout";
        logoutMenuItem.Name = MainApplication.getInstance().getResources().getString(R.string.signout_string);
        menu.add(logoutMenuItem);

        mDrawerList.setAdapter(new ViewsAdapter(menu));

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int index, long id) {

                if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START)) {
                    mDrawerLayout.closeDrawer(Gravity.START);
                }

                if (index == 0) {
                    Intent intent = new Intent(MainApplication.getInstance(), HomescreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("queue")) {
                    Intent intent = new Intent(MainApplication.getInstance(), PlaylistActivity.class);
                    startActivity(intent);
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("channels")) {
                    Intent intent = new Intent(MainApplication.getInstance(), ChannelsActivity.class);
                    startActivity(intent);
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("livetv")) {
                    Intent intent = new Intent(MainApplication.getInstance(), LiveTvActivity.class);
                    startActivity(intent);
                } else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("music")) {
                    Intent intent = new Intent(MainApplication.getInstance(), MusicActivity.class);
                    intent.putExtra("ParentId", menu.get(index).Id);
                    startActivity(intent);

                }  else if (menu.get(index).CollectionType != null && menu.get(index).CollectionType.equalsIgnoreCase("logout")) {
                    closeDrawer();
                    terminateQueuedMedia();
                    MainApplication.getInstance().getConnectionManager().Logout(new EmptyResponse() {

                        @Override
                        public void onResponse() {
                            showUserSelection();
                        }
                    });
                }
                else {

                    String jsonData = MainApplication.getInstance().getJsonSerializer().SerializeToString(menu.get(index));
                    Intent intent = new Intent(MainApplication.getInstance(), LibraryPresentationActivity.class);
                    intent.putExtra("Item", jsonData);
                    startActivity(intent);
                }
            }
        });

    }

    private boolean queueHasItems() {
        return MainApplication.getInstance().PlayerQueue != null
                && MainApplication.getInstance().PlayerQueue.PlaylistItems != null
                && MainApplication.getInstance().PlayerQueue.PlaylistItems.size() > 0;
    }

    private void getLibraryRoot() {
        MainApplication.getInstance().API.GetUserViews(MainApplication.getInstance().API.getCurrentUserId(), getLibraryResponse);
    }


    Response<ItemsResult> getLibraryResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            AppLogger.getLogger().Info(TAG + ": Get library response received");

            if (response != null && response.getTotalRecordCount() > 0) {
                AppLogger.getLogger().Info(TAG + ": library contents available");
                buildNavigationMenu(response.getItems());
            } else {
                AppLogger.getLogger().Info(TAG + ": no library contents in response");
                buildNavigationMenu(null);
            }
        }
        @Override
        public void onError(Exception ex) {
            AppLogger.getLogger().Error(TAG + ": Error getting library contents");
            buildNavigationMenu(null);
        }
    };
}
