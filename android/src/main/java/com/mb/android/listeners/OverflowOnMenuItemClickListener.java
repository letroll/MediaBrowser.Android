package com.mb.android.listeners;

import android.content.Context;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import com.mb.android.MainApplication;
import com.mb.android.Playlist;
import com.mb.android.PlaylistItem;
import com.mb.android.R;
import mediabrowser.apiinteraction.Response;
import com.mb.android.ui.mobile.musicartist.GetInstantMixResponse;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.SimilarItemsByNameQuery;
import mediabrowser.model.querying.SimilarItemsQuery;

import java.util.ArrayList;

/**
 * Created by Mark on 2014-07-21.
 *
 * Class that handles the click event that is generated when the user clicks an overflow menu item
 */
public class OverflowOnMenuItemClickListener
        implements android.support.v7.widget.PopupMenu.OnMenuItemClickListener {

    private BaseItemDto mItem;
    private Context mContext;
    private BaseAdapter mBaseAdapter;

    public OverflowOnMenuItemClickListener(BaseItemDto item, BaseAdapter baseAdapter, Context context) {
        mItem = item;
        mBaseAdapter = baseAdapter;
        mContext = context;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        String menuTitle = item.getTitle().toString();

        if (mContext.getResources().getString(R.string.playlist_remove_string).equalsIgnoreCase(menuTitle)) {

            // Remove from playlist
            removeItemFromPlaylist();

        } else if (mContext.getResources().getString(R.string.playlist_add_string).equalsIgnoreCase(menuTitle)) {

            // Add to playlist
            addItemToPlaylist();

        } else if (mContext.getResources().getString(R.string.instant_mix_string).equalsIgnoreCase(menuTitle)) {

            // Instant Mix
            requestInstantMix();

        } else if (mContext.getResources().getString(R.string.like_string).equalsIgnoreCase(menuTitle)) {

            // Like Item
            setItemLiked();

        } else if (mContext.getResources().getString(R.string.dislike_string).equalsIgnoreCase(menuTitle)) {

            // Dislike Item
            setItemDisliked();

        } else if (mContext.getResources().getString(R.string.clear_rating_string).equalsIgnoreCase(menuTitle)) {

            // Clear Item Rating
            clearItemRating();

        } else if (mContext.getResources().getString(R.string.favorite_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Set Item as Favorite
            setItemFavorite();

        } else if (mContext.getResources().getString(R.string.un_favorite_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Remove Item Favorite Rating
            removeItemFavorite();

        } else if (mContext.getResources().getString(R.string.un_played_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Set Item Unplayed
            setItemUnplayed();

        } else if (mContext.getResources().getString(R.string.played_action_bar_button).equalsIgnoreCase(menuTitle)) {

            // Set Item Played
            setItemPlayed();
        }
        return false;
    }

    /**
     * Adds the current item to the current player queue
     */
    private void addItemToPlaylist() {

        if (MainApplication.getInstance().PlayerQueue == null) {
            MainApplication.getInstance().PlayerQueue = new Playlist();
        }

        if (MainApplication.getInstance().PlayerQueue.PlaylistItems == null) {
            MainApplication.getInstance().PlayerQueue.PlaylistItems = new ArrayList<>();
        }

        if (mItem.getRecursiveItemCount() != null && mItem.getRecursiveItemCount() > 0) {

            ItemQuery query = new ItemQuery();
            query.setParentId(mItem.getId());
            query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
            query.setRecursive(true);
            query.setIncludeItemTypes(new String[] { "audio" });

            MainApplication.getInstance().API.GetItemsAsync(query, getAllRecursiveChildrenResponse);

        } else {
            addItemToPlaylistInternal(mItem);
        }
    }

    private void addItemToPlaylistInternal(BaseItemDto item) {

        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.Id = item.getId();
        playlistItem.Name = item.getName();
        playlistItem.Runtime = item.getRunTimeTicks();
        playlistItem.Type = item.getType();

        if (item.getType() != null && item.getType().equalsIgnoreCase("audio") && item.getArtists() != null) {
            playlistItem.SecondaryText = item.getArtists().get(0);
        }

        MainApplication.getInstance().PlayerQueue.PlaylistItems.add(playlistItem);
    }

    /**
     * Remove the current item from the current player queue
     */
    private void removeItemFromPlaylist() {

        if (MainApplication.getInstance().PlayerQueue == null
                || MainApplication.getInstance().PlayerQueue.PlaylistItems == null) {
            return;
        }

        int index = -1;

        for (int i = 0; i < MainApplication.getInstance().PlayerQueue.PlaylistItems.size(); i++) {
            if (MainApplication.getInstance().PlayerQueue.PlaylistItems.get(i).Id.equalsIgnoreCase(mItem.getId())) {
                index = i;
                break;
            }
        }

        if (index == -1) return;

        MainApplication.getInstance().PlayerQueue.PlaylistItems.remove(index);
    }

    /**
     * Request an Instant Mix from the current Item. Item must be a song, album, artist or music genre
     */
    private void requestInstantMix() {

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(mItem.getType())) return;

        if (mItem.getType().equalsIgnoreCase("Audio")) {

            SimilarItemsQuery query = new SimilarItemsQuery();
            query.setId(mItem.getId());
            query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
            query.setLimit(50);
            MainApplication.getInstance().API.GetInstantMixFromSongAsync(query, new GetInstantMixResponse(mContext));

        } else if (mItem.getType().equalsIgnoreCase("MusicAlbum")) {

            SimilarItemsQuery query = new SimilarItemsQuery();
            query.setId(mItem.getId());
            query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
            query.setLimit(50);
            MainApplication.getInstance().API.GetInstantMixFromAlbumAsync(query, new GetInstantMixResponse(mContext));

        } else if (mItem.getType().equalsIgnoreCase("musicartist")) {

            SimilarItemsByNameQuery query = new SimilarItemsByNameQuery();
            query.setName(mItem.getName());
            query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
            query.setLimit(50);

            MainApplication.getInstance().API.GetInstantMixFromArtistAsync(query, new GetInstantMixResponse(mContext));

        } else if (mItem.getType().equalsIgnoreCase("musicgenre")) {

            SimilarItemsByNameQuery query = new SimilarItemsByNameQuery();
            query.setName(mItem.getName());
            query.setUserId(MainApplication.getInstance().API.getCurrentUserId());
            query.setLimit(50);

            MainApplication.getInstance().API.GetInstantMixFromMusicGenreAsync(query, new GetInstantMixResponse(mContext));
        }
    }

    /**
     * Set the current item as Liked for the current logged in user
     */
    private void setItemLiked() {

        MainApplication.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(), true, new UpdateUserDataResponse());
    }

    /**
     * Set the current item as Disliked for the current logged in user
     */
    private void setItemDisliked() {

        MainApplication.getInstance().API.UpdateUserItemRatingAsync(mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(), false, new UpdateUserDataResponse());
    }

    /**
     * Remove the Liked or Disliked status from the item for the current user
     */
    private void clearItemRating() {

        MainApplication.getInstance().API.ClearUserItemRatingAsync(mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(), new UpdateUserDataResponse());
    }

    /**
     * Set current item as a favorite for the current user
     */
    private void setItemFavorite() {

        MainApplication.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(), true, new UpdateUserDataResponse());
    }

    /**
     * Remove the current item as a favorite for the current user
     */
    private void removeItemFavorite() {

        MainApplication.getInstance().API.UpdateFavoriteStatusAsync(mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(), false, new UpdateUserDataResponse());
    }

    /**
     * Set Item played for the current user
     */
    private void setItemPlayed() {

        MainApplication.getInstance().API.MarkPlayedAsync(
                mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(),
                null,
                new UpdateUserDataResponse()
        );
    }

    /**
     * Set Item played for the current user
     */
    private void setItemUnplayed() {

        MainApplication.getInstance().API.MarkUnplayedAsync(
                mItem.getId(),
                MainApplication.getInstance().API.getCurrentUserId(),
                new UpdateUserDataResponse()
        );
    }

    //**********************************************************************************************
    // Callbacks
    //**********************************************************************************************

    /**
     * Callback that is triggered after the user updates the user rating
     */
    private class UpdateUserDataResponse extends Response<UserItemDataDto> {
        @Override
        public void onResponse(UserItemDataDto userItemDataDto) {

            if (userItemDataDto == null) return;

            mItem.setUserData(userItemDataDto);
            mBaseAdapter.notifyDataSetChanged();
        }
    }

    private Response<ItemsResult> getAllRecursiveChildrenResponse = new Response<ItemsResult>() {

        @Override
        public void onResponse(ItemsResult response) {
            if (response == null || response.getItems() == null) return;

            for (BaseItemDto item : response.getItems()) {
                addItemToPlaylistInternal(item);
            }
        }
        @Override
        public void onError(Exception ex) {

        }
    };
}
