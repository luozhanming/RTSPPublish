package cn.com.ava.rtspserver.network.DDNS;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

/**
 * Implements the Dynu client (https://www.dynu.com).
 */
public class DynuClient extends DDNSClient {
    /**
     * Creates a new DynuClient object.
     *
     * @param context  the context where the NoIpClient is used
     * @param hostname the public hostname handled by the DDNS service
     * @param username the DDNS service username
     * @param password the DDNS service password
     */
    public DynuClient(@NotNull Context context, String hostname, String username, String password) {
        super(context, hostname, username, password);
        mURL = "https://api.dynu.com/nic/update";
    }
}
