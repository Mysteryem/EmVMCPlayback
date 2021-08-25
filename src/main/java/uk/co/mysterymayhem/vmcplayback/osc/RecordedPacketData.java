package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCPacket;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Common interface for recorded OSC packet data.
 * <p>
 * Created by Mysteryem on 11/08/2021.
 */
public interface RecordedPacketData<T extends RecordedPacketData<T>> {
    /**
     * Get the number of OSC messages present in this data.
     *
     * @return the number of OSC messages present in this data
     */
    int getMessageCount();

    /**
     * Convert this to an OSCPacket so it can be sent via OSC.
     *
     * @return this packet data converted to an OSCPacket
     */
    OSCPacket toOscPacket();

    /**
     * Filter this data's messages, discarding messages that fail the predicate and returning null if no messages remain
     * after the filtering.
     *
     * @param messagePredicate predicate to use when filtering, messages that fail the predicate will be discarded.
     * @return A RecordedPacketData of the same type as this, but with messages filtered, may return the same object if
     * all messages pass the filter. If all messages get filtered, returns null.
     */
    T filter(Predicate<RecordedMessage> messagePredicate);

    T mapMessages(Function<RecordedMessage, RecordedMessage> mapper);
}
