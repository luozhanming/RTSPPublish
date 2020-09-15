package cn.com.ava.rtspserver.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Defines the RTP packetizer to stream AAC audio over a TCP channel.<br>
 * It implements RTP/RTCP over RTSP as defined in RFC 2326, session 10.12.<br>
 * Stream data such as RTP packets is encapsulated by an ASCII dollar
 * sign (24 hexadecimal), followed by a one-byte channel identifier,
 * followed by the length of the encapsulated binary data as a binary,
 * two-byte integer in network byte order. The stream data follows
 * immediately afterwards, without a CRLF, but including the upper-layer
 * protocol headers. Each $ block contains exactly one upper-layer
 * protocol data unit, e.g., one RTP packet.
 */
public class TCPAudioPacketizer extends RTPAudioPacketizer {

    private static final int RTP_PACKET_SIZE = 2000;

    private final byte[] mPacket;                   // The RTP packet
    private final int mRTPChannel;                  // The channel to use to send RTP packets
    private final int mRTCPChannel;                 // The channel to use to send RTCP packets

    /**
     * Creates a new TCPAudioPacketizer object.
     *
     * @param connection  the StreamConnection that owns the packetizer
     * @param rtpChannel  the channel used to send RTP packets
     * @param rtcpChannel the channel used to send RTCP packets
     * @param clock       the clock rate in Hz
     * @param seq         the sequence number of the first packet
     */
    public TCPAudioPacketizer(@NotNull StreamConnection connection,
                              int rtpChannel, int rtcpChannel,
                              int clock, int seq) {
        super(connection, clock, RTP_PACKET_SIZE, seq);
        mPacket = new byte[4 + RTP_PACKET_SIZE];
        mRTPChannel = rtpChannel;
        mRTCPChannel = rtcpChannel;
        mPacket[0] = '$';
    }

    @Override
    protected void rtpSend(byte[] data, int length) throws IOException {
        mPacket[1] = (byte) mRTPChannel;
        mPacket[2] = (byte) (length >> 8);
        mPacket[3] = (byte) length;
        System.arraycopy(data, 0, mPacket, 4, length);
        mConnection.write(mPacket, 0, 4 + length);
    }

    @Override
    protected void rtcpSend(byte[] data, int length) throws IOException {
        mPacket[1] = (byte) (mRTCPChannel);
        mPacket[2] = (byte) (length >> 8);
        mPacket[3] = (byte) length;
        System.arraycopy(data, 0, mPacket, 4, length);
        mConnection.write(mPacket, 0, 4 + length);
    }
}
