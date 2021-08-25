package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plays back recorded OSC packets, sending them as new messages to a specified host and port.
 * <p>
 * Created by Mysteryem on 31/07/2021.
 */
public class OscPlayer {
    protected final OSCPortOut oscPortOut;
    private final List<RecordedPacket<?, ?>> recordedPackets;
    private final long repeatPeriod;
    private final Timer timer;
    private final boolean hasMessages;

    private boolean started = false;
    private boolean stopped = false;

    public OscPlayer(int portOut, List<RecordedPacket<?, ?>> recordedPackets, long repeatPeriodMillis) throws IOException {
        this(new InetSocketAddress("localhost", portOut), recordedPackets, repeatPeriodMillis, false);
    }

    public OscPlayer(int portOut, List<RecordedPacket<?, ?>> recordedPackets) throws IOException {
        this(new InetSocketAddress("localhost", portOut), recordedPackets, -1, true);
    }

    public OscPlayer(SocketAddress socketAddress, List<RecordedPacket<?, ?>> recordedPackets, long repeatPeriodMillis) throws IOException {
        this(socketAddress, recordedPackets, repeatPeriodMillis, false);
    }

    public OscPlayer(SocketAddress socketAddress, List<RecordedPacket<?, ?>> recordedPackets) throws IOException {
        this(socketAddress, recordedPackets, -1, true);
    }

    private OscPlayer(SocketAddress socketAddress, List<RecordedPacket<?, ?>> recordedPackets, long repeatPeriodMillis, boolean autoDuration) throws IOException {
        if (recordedPackets == null) {
            throw new IllegalArgumentException("Recorded packets must not be null");
        }

        if (!recordedPackets.isEmpty()) {

            this.oscPortOut = new OSCPortOut(socketAddress);

            // Should be sorted already if come from a direct recording, but might not be if read from a file
            this.recordedPackets = recordedPackets.stream()
                    .sorted(Comparator.comparingLong(RecordedPacket::getOffsetTime))
                    .collect(Collectors.toList());
            RecordedPacket<?, ?> lastRecordedPacket = this.recordedPackets.get(this.recordedPackets.size() - 1);
            if (lastRecordedPacket != null) {
                long greatestOffsetTime = lastRecordedPacket.getOffsetTime();
                if (!autoDuration) {
                    if (greatestOffsetTime > repeatPeriodMillis) {
                        throw new IllegalArgumentException("Repeat duration cannot be less than the greatest offset time");
                    }
                } else {
                    repeatPeriodMillis = greatestOffsetTime;
                    if (repeatPeriodMillis < 1) {
                        throw new IllegalArgumentException("repeat duration must be positive");
                    }
                }
            } else {
                // There's no last recorded message, so there's no messages at all
                // Maybe print out something
            }
            this.repeatPeriod = repeatPeriodMillis;
            this.timer = new Timer("OscOutTimer");
            this.hasMessages = true;

        } else {
            System.out.println("Note, recorded packets is empty");
            this.recordedPackets = Collections.emptyList();
            this.repeatPeriod = -1;
            this.timer = null;
            this.hasMessages = false;
            this.oscPortOut = null;
        }
    }

    public void start() {
        if (this.started) {
            throw new IllegalStateException("Already started");
        }
        if (this.hasMessages) {
            this.addRecordedMessages();
        }
        this.started = true;
    }

    public void stop() {
        if (this.stopped) {
            throw new IllegalStateException("Already stopped");
        }
        if (!this.started) {
            throw new IllegalStateException("Hasn't started yet");
        }
        if (this.hasMessages) {
            this.timer.cancel();
        }
        this.stopped = true;
    }

    protected void addRecordedMessages() {
        // It might be better to map entirely first and then schedule the tasks
        this.recordedPackets.stream()
                .map(recordedPacket -> new RecordedPacketTimerTask(recordedPacket, this.oscPortOut))
                .forEach(rmtt -> this.scheduleTask(rmtt, rmtt.getDelayMillis(), this.repeatPeriod));
    }

    protected void scheduleTask(TimerTask task, long delayMillis, long repeatPeriodMillis) {
        this.timer.scheduleAtFixedRate(task, delayMillis, repeatPeriodMillis);
    }
}
