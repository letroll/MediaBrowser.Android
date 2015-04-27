package com.mb.android;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 11/12/13.
 */
public class Playlist {

    /**
     * List of items to play. List is used even if only one item is being played
     */
    public List<PlaylistItem> PlaylistItems;

    /**
     * Class Constructor
     */
    public Playlist() {
        PlaylistItems = new ArrayList<>();
    }
}
