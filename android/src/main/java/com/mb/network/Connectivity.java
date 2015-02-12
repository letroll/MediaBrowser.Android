package com.mb.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Mark on 11/12/13.
 */
public class Connectivity {

    public static boolean isConnectedLAN(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


        NetworkInfo mNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mNetInfo != null && mNetInfo.isConnected())
            return true;

        mNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        if (mNetInfo != null && mNetInfo.isConnected())
            return true;

        return false;
    }
}
