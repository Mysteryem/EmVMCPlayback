package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortInBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.Predicate;

/**
 * Records OSC messages received on a specified port and allows filtering which messages get recorded.
 * <p>
 * Created by Mysteryem on 31/07/2021.
 */
public class OscRecorder {

    private final RecordingPacketListener recordingPacketListener;
    private final OSCPortIn oscPortIn;
    private boolean started = false;
    private boolean ended = false;
    private long endTime = -1;

    public OscRecorder(Predicate<RecordedMessage> messageSelector, SocketAddress socketAddress) throws IOException {
        RecordingPacketListener messageListener = new RecordingPacketListener(messageSelector);

        this.recordingPacketListener = messageListener;
        this.oscPortIn = new OSCPortInBuilder()
                .setSocketAddress(socketAddress)
                .addPacketListener(messageListener)
                .build();
    }

    public OscRecorder(Predicate<RecordedMessage> messageSelector, String hostname, int port) throws IOException {
        this(messageSelector, new InetSocketAddress(hostname, port));
    }

    public OscRecorder(Predicate<RecordedMessage> messageSelector, InetAddress inetAddress, int port) throws IOException {
        this(messageSelector, new InetSocketAddress(inetAddress, port));
    }

    public OscRecorder(Predicate<RecordedMessage> messageSelector, int port) throws IOException {
        this(messageSelector, "localhost", port);
    }

    public void init() {
        this.oscPortIn.run();
    }

    public void startRecording() {
        this.recordingPacketListener.startRecording();
        this.oscPortIn.startListening();
        this.started = true;
    }

    public List<RecordedPacket<?, ?>> stopRecording() throws IOException {
        if (this.started) {
            this.oscPortIn.stopListening();
            long endTimeMilli = System.currentTimeMillis();
            this.oscPortIn.close();
            this.endTime = endTimeMilli;
            this.ended = true;
            return recordingPacketListener.getRecordedPackets();
        } else {
            throw new IllegalStateException("Can't stop recording when not started yet");
        }
    }

    public long getStartTimeMillis() {
        if (this.started) {
            return this.recordingPacketListener.getStartTime();
        } else {
            throw new IllegalStateException("Can't get start time when not started yet");
        }
    }

    public long getEndTimeMillis() {
        if (this.ended) {
            return this.endTime;
        } else {
            throw new IllegalStateException("Can't get end time when not ended yet");
        }
    }

    public long getRecordingDurationMillis() {
        if (this.started) {
            if (this.ended) {
                return this.getEndTimeMillis() - this.getStartTimeMillis();
            } else {
                throw new IllegalStateException("Can't get recording time when not ended yet");
            }
        } else {
            throw new IllegalStateException("Can't get recording time when not started yet");
        }
    }

    public int getPacketCount() {
        return recordingPacketListener.getPacketCount();
    }

    public int countMessages() {
        return recordingPacketListener.countMessages();
    }
}
