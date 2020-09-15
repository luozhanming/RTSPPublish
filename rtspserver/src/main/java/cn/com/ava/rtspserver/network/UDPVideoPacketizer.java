package cn.com.ava.rtspserver.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Defines the RTP packetizer to stream AVC video slices using the UDP protocol.
 */
public class UDPVideoPacketizer extends RTPVideoPacketizer {

    protected static final int RTP_PACKET_SIZE = 1400;

    private final DatagramSocket mRTPSocket;        // The socket to send RTP packets to
    private final DatagramSocket mRTCPSocket;       // The socket to send RTCP packets to
    private final DatagramPacket mPacket;           // The UDP packet to send
    private final int mRTPPort;                     // The client port used by the RTP protocol
    private final int mRTCPPort;                    // The client port used by the RTCP protocol

    /**
     * Creates a new UDPVideoPacketizer object.
     *
     * @param connection the StreamConnection that owns the packetizer
     * @param host       the host to send packets to
     * @param rtpPort    the UDP port to send RTP packets to
     * @param rtcpPort   the UDP port to send RTCP packets to
     * @param clock      the clock rate in Hz
     * @param seq        the sequence number of the first packet
     */
    public UDPVideoPacketizer(@NotNull StreamConnection connection,
                              InetAddress host, int rtpPort, int rtcpPort,
                              int clock, int seq)
            throws SocketException {
        super(connection, clock, RTP_PACKET_SIZE, seq);
        mRTPSocket = new DatagramSocket();
        mRTCPSocket = new DatagramSocket(mRTPSocket.getLocalPort() + 1);
        mPacket = new DatagramPacket(new byte[RTP_PACKET_SIZE], RTP_PACKET_SIZE);
        mPacket.setAddress(host);
        mRTPPort = rtpPort;
        mRTCPPort = rtcpPort;
    }

    @Override
    protected void rtpSend(byte[] data, int length) throws IOException {
        mPacket.setData(data, 0, length);
        mPacket.setPort(mRTPPort);
        mRTPSocket.send(mPacket);
    }

    @Override
    protected void rtcpSend(byte[] data, int length) throws IOException {
        mPacket.setData(data, 0, length);
        mPacket.setPort(mRTCPPort);
        mRTCPSocket.send(mPacket);
    }

    @Override
    public void close() {
        super.close();
        mRTPSocket.close();
        mRTCPSocket.close();
    }

    /**
     * @return the local port used by the RTP protocol
     */
    public int getRTPLocalPort() {
        return mRTPSocket.getLocalPort();
    }

    /**
     * @return the local port used by the RTCP protocol
     */
    public int getRTCPLocalPort() {
        return mRTCPSocket.getLocalPort();
    }
}
