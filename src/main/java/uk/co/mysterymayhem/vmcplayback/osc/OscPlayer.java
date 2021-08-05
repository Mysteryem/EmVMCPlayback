package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class OscPlayer {
    private final List<RecordedMessage> recordedMessages;
    private final OSCPortOut oscPortOut;
    private final long repeatPeriod;
    private final Timer timer;
    private final boolean hasMessages;

    private boolean started = false;
    private boolean stopped = false;

    public OscPlayer(int portOut, List<RecordedMessage> recordedMessages, long repeatPeriodMillis) throws IOException {
        this(new InetSocketAddress("localhost", portOut), recordedMessages, repeatPeriodMillis, false);
    }

    public OscPlayer(int portOut, List<RecordedMessage> recordedMessages) throws IOException {
        this(new InetSocketAddress("localhost", portOut), recordedMessages, -1, true);
    }

    public OscPlayer(SocketAddress socketAddress, List<RecordedMessage> recordedMessages, long repeatPeriodMillis) throws IOException {
        this(socketAddress, recordedMessages, repeatPeriodMillis, false);
    }

    public OscPlayer(SocketAddress socketAddress, List<RecordedMessage> recordedMessages) throws IOException {
        this(socketAddress, recordedMessages, -1, true);
    }

    private OscPlayer(SocketAddress socketAddress, List<RecordedMessage> recordedMessages, long repeatPeriodMillis, boolean autoDuration) throws IOException {
        if (recordedMessages == null) {
            throw new IllegalArgumentException("Recorded messages must not be null");
        }

        if (!recordedMessages.isEmpty()) {

            this.oscPortOut = new OSCPortOut(socketAddress);

            // Should be sorted already if come from a direct recording, but might not be if read from a file
            this.recordedMessages = recordedMessages.stream()
                    .sorted(Comparator.comparingLong(RecordedMessage::getOffsetTime))
                    .collect(Collectors.toList());
            RecordedMessage lastRecordedMessage = this.recordedMessages.get(this.recordedMessages.size() - 1);
            if (lastRecordedMessage != null) {
                long greatestOffsetTime = lastRecordedMessage.getOffsetTime();
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
            System.out.println("Note, recorded messages is empty");
            this.recordedMessages = Collections.emptyList();
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
            // It might be better to map entirely first and then schedule the tasks
            this.recordedMessages.stream()
                    .map(recordedMessage -> new RecordedMessageTimerTask(recordedMessage, this.oscPortOut))
                    .forEach(rmtt -> this.timer.scheduleAtFixedRate(rmtt, rmtt.getRecordedMessage().getOffsetTime(), this.repeatPeriod));
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
}
