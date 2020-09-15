package cn.com.ava.rtspserver.network.DDNS;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

/**
 * Implements the NoIp client (https://my.noip.com).
 */
public class NoIpClient extends DDNSClient {
    /**
     * Creates a new NoIpClient object.
     *
     * @param context  the context where the NoIpClient is used
     * @param hostname the public hostname handled by the DDNS service
     * @param username the DDNS service username
     * @param password the DDNS service password
     */
    public NoIpClient(@NotNull Context context, String hostname, String username, String password) {
        super(context, hostname, username, password);
        mURL = "https://dynupdate.no-ip.com/nic/update";
    }
}
