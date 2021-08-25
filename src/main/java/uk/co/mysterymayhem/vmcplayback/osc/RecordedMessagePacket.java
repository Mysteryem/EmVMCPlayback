package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCMessage;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents the recording of a packet that contains an OSC message.
 * <p>
 * Created by Mysteryem on 11/08/2021.
 */
public class RecordedMessagePacket extends RecordedPacket<RecordedMessagePacket, RecordedMessage> implements Serializable {
    private static final long serialVersionUID = 1L;

    public RecordedMessagePacket(long offsetTime, OSCMessage oscMessage) {
        this(offsetTime, new RecordedMessage(oscMessage));
    }

    public RecordedMessagePacket(long offsetTime, RecordedMessage recordedMessage) {
        super(offsetTime, recordedMessage);
    }

    @Override
    public RecordedMessagePacket filter(Predicate<RecordedMessage> messagePredicate) {
        RecordedMessage filtered = this.filterData(messagePredicate);
        if (filtered != null) {
            return new RecordedMessagePacket(this.getOffsetTime(), filtered);
        } else {
            return null;
        }
    }

    @Override
    public RecordedMessagePacket mapMessages(Function<RecordedMessage, RecordedMessage> mapper) {
        RecordedMessage packetData = this.getPacketData();
        RecordedMessage newPacketData = mapper.apply(packetData);
        if (!packetData.equals(newPacketData)) {
            this.setPacketData(newPacketData);
        }
        return this;
    }
}
