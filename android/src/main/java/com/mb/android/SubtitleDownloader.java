package com.mb.android;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;

import com.mb.android.logging.AppLogger;
import com.mb.android.ui.main.ConnectionActivity;
import mediabrowser.apiinteraction.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Created by Mark on 2014-10-23.
 */
public class SubtitleDownloader extends AsyncTask<String, String, File> {

    private static final String TAG = "AsyncGet";
    private HttpURLConnection mConnection;
    private Response mResponse;
    private int defaultTimeoutMilliseconds;

    public SubtitleDownloader(Response response) {

        this.mResponse = response;
        this.defaultTimeoutMilliseconds = 10000;
    }

    @Override
    protected File doInBackground(String... uri) {

        try {

            URL url = new URL(uri[0]);
            mConnection = (HttpURLConnection) url.openConnection();
            mConnection.setConnectTimeout(defaultTimeoutMilliseconds);
            mConnection.setReadTimeout(300000);
            mConnection.addRequestProperty("Accept-Encoding", "gzip");
            mConnection.addRequestProperty("Cache-Control", "no-cache");
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getAccessToken())) {
                mConnection.addRequestProperty("X-MediaBrowser-Token", MainApplication.getInstance().API.getAccessToken());
            }
            String header = getAuthorizationParameter();
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(header)) {
                mConnection.addRequestProperty("Authorization", "MediaBrowser" + " " + header);
            }
            mConnection.connect();

            try {
                if (!succeeded(mConnection.getResponseCode())) {
                    this.cancel(true);
                    return null;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            String encoding = mConnection.getContentEncoding();

            InputStream inStream;
            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                inStream = new GZIPInputStream(mConnection.getInputStream());
            } else {
                inStream = mConnection.getInputStream();
            }

            if (inStream != null) {

                OutputStream output = null;

                try {

                    File folder = Environment.getExternalStorageDirectory();
                    File subFile = new File(folder.getAbsolutePath() + File.separator + "tempSub.srt");

                    output = new FileOutputStream(subFile);
                    byte[] buf = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buf)) > 0) {
                        output.write(buf, 0, bytesRead);
                    }

                    return subFile;

                } catch (Exception e) {
                    AppLogger.getLogger().Info(TAG, "Exception");
                    AppLogger.getLogger().ErrorException("Exception: ", e);
                    if (e.getMessage() != null) {
                        AppLogger.getLogger().Error(e.getMessage());
                    }
                } finally {
                    inStream.close();
                    if (output != null) {
                        output.close();
                    }
                }
            }

        } catch (IOException e) {
            AppLogger.getLogger().ErrorException(TAG + ", Exception handled. ", e);

            if (e.getMessage() != null) {
                AppLogger.getLogger().Info(TAG, e.getMessage());
            }

        } finally {
            mConnection.disconnect();
        }

        return null;
    }

    @Override
    protected void onPostExecute(File subtitleFile) {

        if (!isCancelled())
            mResponse.onResponse(subtitleFile);
    }

    private boolean succeeded(int responseCode) {

        if (responseCode == 401) {
            // Token has been revoked.
            MainApplication.getInstance().API.SetAuthenticationInfo(null, null);

            Intent intent = new Intent(MainApplication.getInstance(), ConnectionActivity.class);
            intent.putExtra("ShowUserList", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            MainApplication.getInstance().startActivity(intent);

            return false;
        }

        return true;
    }

    private final String getAuthorizationParameter()
    {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getClientName()) && tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getDeviceId()) && tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getDeviceName()))
        {
            return "";
        }

        //C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
        String header = String.format("Client=\"%1$s\", DeviceId=\"%2$s\", Device=\"%3$s\", Version=\"%4$s\"", MainApplication.getInstance().API.getClientName(), MainApplication.getInstance().API.getDeviceId(), MainApplication.getInstance().API.getDeviceName(), MainApplication.getInstance().API.getApplicationVersion());

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(MainApplication.getInstance().API.getCurrentUserId()))
        {
            header += String.format(", UserId=\"%1$s\"", MainApplication.getInstance().API.getCurrentUserId());
        }

        return header;
    }
}
