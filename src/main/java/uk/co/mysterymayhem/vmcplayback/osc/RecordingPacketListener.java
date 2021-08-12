package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * Packet listener that records received OSC packets.
 * <p>
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordingPacketListener implements OSCPacketListener {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingPacketListener.class);
    private final Predicate<RecordedMessage> messageSelector;
    private int packetCount;
    private int messageCount;
    private long startTime;
    private ArrayList<RecordedPacket<?, ?>> recordedPackets = new ArrayList<>();

    public RecordingPacketListener(Predicate<RecordedMessage> messageSelector) {
        this.messageSelector = messageSelector;
    }

    public void startRecording() {
        // ditch any old messages
        this.recordedPackets = new ArrayList<>();
        // get the time now for use when calculating time offsets of when messages have been received
        this.startTime = System.currentTimeMillis();
    }

    public int getPacketCount() {
        return packetCount;
    }

    public int countMessages() {
        return messageCount;
        // Could calculate with this instead of counting as the packets get handled
        //return this.recordedPackets.stream().map(RecordedPacket::getPacketData).mapToInt(RecordedPacketData::getMessageCount).sum();
    }

    public long getStartTime() {
        return startTime;
    }

    public ArrayList<RecordedPacket<?, ?>> getRecordedPackets() {
        return recordedPackets;
    }

    @Override
    public void handlePacket(OSCPacketEvent event) {
        long timeReceived = System.currentTimeMillis();
        packetCount++;
        long offsetTime = timeReceived - this.startTime;
        OSCPacket packet = event.getPacket();
        if (packet instanceof OSCMessage) {
            OSCMessage message = (OSCMessage) packet;

            RecordedMessagePacket recordedMessagePacket = new RecordedMessagePacket(offsetTime, message);
            if (this.messageSelector.test(recordedMessagePacket.getPacketData())) {
                this.recordedPackets.add(recordedMessagePacket);
                // Should always be +1 since OSCMessages have a single message
                this.messageCount += recordedMessagePacket.getPacketData().getMessageCount();
            }
        } else if (packet instanceof OSCBundle) {
            OSCBundle bundle = (OSCBundle) packet;

            RecordedBundlePacket recordedBundlePacket = RecordedBundlePacket.fromOscBundle(offsetTime, bundle, this.messageSelector);
            // If the bundle has no recorded packet data (it has no messages), ignore it
            if (recordedBundlePacket != null && !recordedBundlePacket.getPacketData().getRecordedPacketData().isEmpty()) {
                this.recordedPackets.add(recordedBundlePacket);
                this.messageCount += recordedBundlePacket.getPacketData().getMessageCount();
            }
        } else {
            throw new RuntimeException("Unexpected OSCPacket '" + packet + "' of class '" + packet.getClass() + "'");
        }

    }

    @Override
    public void handleBadData(OSCBadDataEvent event) {
        LOG.warn("Got bad data packet", event.getException());
    }
}
