package cn.com.ava.rtspserver.network.DDNS;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a generic Dynamic DNS client.
 */
public abstract class DDNSClient implements Closeable {

    protected final String TAG = getClass().getSimpleName();

    protected final int TIMEOUT = 5000;

    protected final Context mContext;       // The context that uses the DDNSClient
    protected String mURL;                  // The server address
    protected String mAuthorization;        // The authentication string
    protected String mUserAgent;            // The user-agent string
    protected String mHostname;             // The public hostname handled by the DDNS service
    protected Timer mTimer;                 // The Timer used to schedule DDNS updates
    protected long mPeriod;                 // The update period, in seconds
    protected volatile boolean mIsStarted;  // Whether the DDNSClient is started

    /**
     * Creates a new DDNSClient object.
     *
     * @param context  the context where the DDNSClient is used
     * @param hostname the public hostname handled by the DDNS service
     * @param username the DDNS service username
     * @param password the DDNS service password
     */
    public DDNSClient(Context context, String hostname, String username, String password) {
        mContext = context;
        mURL = "https://api.ipify.org/";
        mAuthorization = "Basic " +
                Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
        mUserAgent = "DDNS Android/1.0.0";
        mHostname = hostname;
    }

    /**
     * Starts, or restart, the DDNS update.
     *
     * @param delay  delay in seconds before update is to be executed
     * @param period time in seconds between successive updates
     */
    public synchronized void start(long delay, long period) {
        close();
        delay *= 1000;
        mPeriod = period * 1000;
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                parseResult(updateDDNS());
            }
        }, delay, mPeriod);
        mIsStarted = true;
    }

    /**
     * Closes the DDNSClient, no more updates will be performed.
     */
    @Override
    public synchronized void close() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mIsStarted = false;
        }
    }

    /**
     * @return {@code true} if the DDNSClient is started, {@code false} otherwise
     */
    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    /**
     * Updates the DDNS service.
     *
     * @return the update result
     */
    protected String updateDDNS() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(mURL + "?" + "hostname=" + mHostname);
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Authorization", mAuthorization);
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
            while ((data = reader.readLine()) != null) result += data + "\n";
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

    /**
     * Parses the uodate result.
     *
     * @param result the update result
     */
    protected void parseResult(String result) {
        if (result == null)
            return;
        String[] s = result.split("[ \n]");
        String message;
        if (s.length > 0) {
            switch (s[0]) {
                // DNS hostname update successful
                case "nochg":
                case "good":
                    message = "Success";
                    break;
                // Invalid username password combination
                case "badauth":
                    close();
                    message = "Invalid username password combination";
                    break;
                // Client disabled, client should exit and not perform any more updates without
                // user intervention
                case "badagent":
                    close();
                    message = "Client disabled";
                    break;
                // Hostname supplied does not exist under specified account, client exit and require
                // user to enter new login credentials before performing an additional request
                case "nohost":
                    close();
                    message = "Hostname supplied does not exist under this account";
                    break;
                // An invalid request is made to the API server
                case "unknown":
                    close();
                    message = "Invalid request to the API server";
                    break;
                // The hostname is not a valid fully qualified hostname
                case "notfqdn":
                    close();
                    message = "The hostname is not a valid fully qualified hostname";
                    break;
                // Too many hostnames are specified for the update process
                case "numhost":
                    close();
                    message = "Too many hostnames are specified for the update process";
                    break;
                // Username is blocked due to abuse, client should stop sending updates.
                case "abuse":
                    close();
                    message = "The account is blocked due to abuse";
                    break;
                // An update request was sent including a feature that is not available to that
                // particular user
                case "!donator":
                    close();
                    message = "Feature not available for this account";
                    break;
                // An error was encountered on the server side, the client may send across
                // the request again to have the request processed successfully
                case "dnserr":
                case "servererror":
                    message = "Server error";
                    break;
                // A fatal error on our side such as a database outage, retry the update
                // no sooner than 30 minutes.
                case "911":
                    start(30 * 60, mPeriod);
                    message = "Fatal error";
                    break;
                default:
                    message = result;
                    break;
            }
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }
    }
}
