package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.MessageSelector;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortInBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class OscRecorder {

    private final RecordingMessageListener recordingMessageListener;
    private final OSCPortIn oscPortIn;
    private boolean started = false;
    private boolean ended = false;
    private long endTime = -1;

    public OscRecorder(MessageSelector messageSelector, SocketAddress socketAddress) throws IOException {
        RecordingMessageListener messageListener = new RecordingMessageListener();

        this.recordingMessageListener = messageListener;
        this.oscPortIn = new OSCPortInBuilder()
                .setSocketAddress(socketAddress)
                .addPacketListener(OSCPortIn.defaultPacketListener())
                .addMessageListener(messageSelector, messageListener)
                .build();
    }

    public OscRecorder(MessageSelector messageSelector, String hostname, int port) throws IOException {
        this(messageSelector, new InetSocketAddress(hostname, port));
    }

    public OscRecorder(MessageSelector messageSelector, InetAddress inetAddress, int port) throws IOException {
        this(messageSelector, new InetSocketAddress(inetAddress, port));
    }

    public OscRecorder(MessageSelector messageSelector, int port) throws IOException {
        this(messageSelector, "localhost", port);
    }

    public void init() {
        this.oscPortIn.run();
    }

    public void startRecording() {
        this.recordingMessageListener.startRecording();
        this.oscPortIn.startListening();
        this.started = true;
    }

    public List<RecordedMessage> stopRecording() throws IOException {
        if (this.started) {
            this.oscPortIn.stopListening();
            long endTimeMilli = System.currentTimeMillis();
            this.oscPortIn.close();
            this.endTime = endTimeMilli;
            this.ended = true;
            return recordingMessageListener.getRecordedMessages();
        } else {
            throw new IllegalStateException("Can't stop recording when not started yet");
        }
    }

    public long getStartTimeMillis() {
        if (this.started) {
            return this.recordingMessageListener.getStartTime();
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

    public int getMessageCount() {
        return recordingMessageListener.getMessageCount();
    }
}