package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents the recording of a packet that contains an OSC bundle.
 * <p>
 * Created by Mysteryem on 11/08/2021.
 */
public class RecordedBundlePacket extends RecordedPacket<RecordedBundlePacket, RecordedBundle> implements Serializable {
    private static final long serialVersionUID = 1L;

    private RecordedBundlePacket(long offsetTime, RecordedBundle recordedBundle) {
        super(offsetTime, recordedBundle);
    }

    public static RecordedBundlePacket fromOscBundle(long offsetTime, OSCBundle oscBundle, Predicate<RecordedMessage> messagePredicate) {

        OscConverter converter = new OscConverter(messagePredicate);
        RecordedBundle recordedBundle = converter.convertOscBundle(oscBundle);
        if (recordedBundle != null) {
            return new RecordedBundlePacket(offsetTime, recordedBundle);
        } else {
            return null;
        }
    }

    // Not used at the moment. The only time RecordedBundlePackets are created outside of this class, and not from OSC
    // classes, is from deserialization
    public static RecordedBundlePacket fromData(long offsetTime, long ntpTime, List<RecordedPacketData<?>> recordedPacketData, Predicate<RecordedMessage> messagePredicate) {
        recordedPacketData = recordedPacketData.stream().map(data -> data.filter(messagePredicate)).filter(Objects::nonNull).collect(Collectors.toList());
        if (recordedPacketData.isEmpty()) {
            return null;
        } else {
            return new RecordedBundlePacket(offsetTime, new RecordedBundle(ntpTime, recordedPacketData));
        }
    }

    @Override
    public RecordedBundlePacket filter(Predicate<RecordedMessage> messagePredicate) {
        RecordedBundle filtered = this.getPacketData().filter(messagePredicate);
        if (filtered != null) {
            return new RecordedBundlePacket(this.getOffsetTime(), filtered);
        } else {
            return null;
        }
    }

    @Override
    public RecordedBundlePacket mapMessages(Function<RecordedMessage, RecordedMessage> mapper) {
        RecordedBundle recordedBundle = this.getPacketData();
        this.setPacketData(recordedBundle.mapMessages(mapper));
        return this;
    }

    private static class OscConverter {
        private final Predicate<RecordedMessage> messagePredicate;

        OscConverter(Predicate<RecordedMessage> messagePredicate) {
            this.messagePredicate = messagePredicate;
        }


        private RecordedPacketData<?> convertOscPacket(OSCPacket oscPacket) {
            if (oscPacket instanceof OSCBundle) {
                return this.convertOscBundle((OSCBundle) oscPacket);
            } else if (oscPacket instanceof OSCMessage) {
                // Null if fails the predicate
                return this.convertOscMessage((OSCMessage) oscPacket);
            } else {
                throw new IllegalArgumentException("Unsupported OSCPacket " + oscPacket);
            }
        }

        private RecordedBundle convertOscBundle(OSCBundle oscBundle) {
            long ntpTime = oscBundle.getTimestamp().getNtpTime();

            // Recursion here
            List<RecordedPacketData<?>> nonNullRecordedPacketData =
                    oscBundle.getPackets()
                            .stream()
                            .map(this::convertOscPacket)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            RecordedBundle recordedBundle = new RecordedBundle(ntpTime, nonNullRecordedPacketData);
            if (recordedBundle.getRecordedPacketData().isEmpty()) {
                // If the bundle is empty (e.g. all of its messages failed the predicate and all of its sub-bundles did also for their messages, we don't care about it
                return null;
            } else {
                return recordedBundle;
            }
        }

        private RecordedMessage convertOscMessage(OSCMessage oscMessage) {
            RecordedMessage message = new RecordedMessage(oscMessage);
            if (this.messagePredicate.test(message)) {
                return message;
            } else {
                return null;
            }
        }
    }
}
