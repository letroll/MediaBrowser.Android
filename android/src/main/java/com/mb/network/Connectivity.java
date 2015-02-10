package com.mb.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import com.mb.android.logging.FileLogger;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.extensions.StringHelper;

import java.util.Formatter;

/**
 * Created by Mark on 11/12/13.
 */
public class Connectivity {
    private final static String[] typicalLanIps = {"192.", "10.", "127."};

    /**
     * Check if there is any connectivity
     *
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

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
