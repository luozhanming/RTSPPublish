package cn.com.ava.rtspserver.network;

/**
 * Created by oywf on 2017/11/27.
 */
public class ServiceConfig {
    //服务端是否需要账号密码
    public static boolean isServerAuthentication = false;
    public static String username = "";
    public static String password = "";
    //是否开启ddns 服务
    public static boolean isServerUpdateDDNS = false;
    public static String ddns_host = "";
    public static String ddns_username = "";
    public static String ddns_password = "";
    //"all" "wifi" "mobile"
    public static String serverDDNSNetworkType="all";


    //是否开启upnp服务
    public static boolean isServerUPnP = false;

}
