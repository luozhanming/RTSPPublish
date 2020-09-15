package cn.com.ava.rtspserver.network.DDNS;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

/**
 * Implements the Duck DNS client (https://www.duckdns.org).
 */
public class DuckDNSClient extends DDNSClient {

    protected String mToken;                // The account token (aka password)

    /**
     * Creates a new DuckDNSClient object.
     *
     * @param context  the context where the NoIpClient is used
     * @param hostname the public hostname handled by the DDNS service
     * @param username the DDNS service username
     * @param password the DDNS service password
     */
    public DuckDNSClient(@NotNull Context context, String hostname, String username, String password) {
        super(context, hostname, username, password);
        mToken = password;
        mURL = "https://www.duckdns.org/update";
    }

    @Override
    protected String updateDDNS() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(mURL + "?" + "domains=" + mHostname + "&token=" + mToken + "&verbose=true");
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", mUserAgent);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            InputStream in;
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String result = "", data;
            while ((data = reader.readLine()) != null) result += data + " ";
            Log.v(TAG, "DDNS update result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "DDNS update request failed", e);
            return null;
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    protected void parseResult(String result) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
    }
}
