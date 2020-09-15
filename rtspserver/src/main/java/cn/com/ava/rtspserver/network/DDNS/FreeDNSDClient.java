package cn.com.ava.rtspserver.network.DDNS;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

/**
 * Implements the Free DNS client (http://freedns.afraid.org/dynamic).
 */
public class FreeDNSDClient extends DDNSClient {
    /**
     * Creates a new DNSdynamicClient object.
     *
     * @param context  the context where the NoIpClient is used
     * @param hostname the public hostname handled by the DDNS service
     * @param username the DDNS service username
     * @param password the DDNS service password
     */
    public FreeDNSDClient(@NotNull Context context, String hostname, String username, String password) {
        super(context, hostname, username, password);
        mURL = "https://sync.afraid.org/nic/update";
    }
}
