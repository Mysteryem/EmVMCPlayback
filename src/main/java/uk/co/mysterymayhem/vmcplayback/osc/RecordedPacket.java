package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCPacket;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Common base class for recorded OSC packets.
 * <p>
 * Created by Mysteryem on 11/08/2021.
 */
// Both "& Serializable" should be removed when our own Serialization is implemented instead of using Java's.
// The extra bounds are only here to keep the Serialization in check
public abstract class RecordedPacket<T extends RecordedPacket<T, U> & Serializable, U extends RecordedPacketData<U> & Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long offsetTime;
    private U packetData;

    public RecordedPacket(long offsetTime, U packetData) {
        this.offsetTime = offsetTime;
        this.packetData = packetData;
    }

    public long getOffsetTime() {
        return offsetTime;
    }

    public U getPacketData() {
        return packetData;
    }

    protected void setPacketData(U newPacketData) {
        this.packetData = newPacketData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordedPacket<?, ?> that = (RecordedPacket<?, ?>) o;
        return offsetTime == that.offsetTime &&
                Objects.equals(packetData, that.packetData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offsetTime, packetData);
    }

    @Override
    public String toString() {
        return "RecordedPacket{" +
                "offsetTime=" + offsetTime +
                ", packetData=" + packetData +
                '}';
    }

    /**
     * Convert this to an OSCPacket so it can be sent via OSC.
     *
     * @return this packet converted to an OSCPacket
     */
    public OSCPacket toOscPacket() {
        return this.getPacketData().toOscPacket();
    }

    /**
     * Filter this packet's messages, discarding messages that fail the given predicate and returning null if no
     * messages remain after filtering.
     *
     * @param messagePredicate Predicate to use when filtering, messages that fail will be discarded
     * @return A RecordedPacket of the same type as this, but with messages filtered, may return the same object if
     */
    public abstract T filter(Predicate<RecordedMessage> messagePredicate);

    protected U filterData(Predicate<RecordedMessage> messagePredicate) {
        return this.getPacketData().filter(messagePredicate);
    }

    @SuppressWarnings("unchecked")
    public T mapMessages(Function<RecordedMessage, RecordedMessage> mapper) {
        U packetData = this.getPacketData();
        U mapped = packetData.mapMessages(mapper);
        if (Objects.equals(packetData, mapped)) {
            this.setPacketData(mapped);
        }
        return (T)this;
    }
}
