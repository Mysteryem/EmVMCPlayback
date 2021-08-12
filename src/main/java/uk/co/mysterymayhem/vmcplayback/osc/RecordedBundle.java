package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.argument.OSCTimeTag64;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents the data of a recorded OSC bundle.
 * <p>
 * Created by Mysteryem on 11/08/2021.
 */
public class RecordedBundle implements RecordedPacketData<RecordedBundle>, Serializable {
    private static final long serialVersionUID = 1L;

    private final long ntpTime;
    // TODO: Make transient and non-final and do custom (de)serialization so that it's not possible to try to serialize
    //  some weird list that either serializes poorly or can't be serialized
    private final List<RecordedPacketData<?>> recordedPacketData;

    public RecordedBundle(long ntpTime, List<RecordedPacketData<?>> recordedPacketData) {
        this.ntpTime = ntpTime;
        this.recordedPacketData = recordedPacketData;
    }

    public long getNtpTime() {
        return ntpTime;
    }

    public List<RecordedPacketData<?>> getRecordedPacketData() {
        return recordedPacketData;
    }

    @Override
    public int getMessageCount() {
        return this.getRecordedPacketData().stream().mapToInt(RecordedPacketData::getMessageCount).sum();
    }

    @Override
    public OSCPacket toOscPacket() {
        List<OSCPacket> packets = this.getRecordedPacketData()
                .stream()
                .map(RecordedPacketData::toOscPacket)
                .collect(Collectors.toList());
        return new OSCBundle(packets, OSCTimeTag64.valueOf(this.getNtpTime()));
    }

    @Override
    public RecordedBundle filter(Predicate<RecordedMessage> messagePredicate) {
        List<RecordedPacketData<?>> filteredData =
                this.getRecordedPacketData()
                        .stream()
                        .map(recordedPacketData -> recordedPacketData.filter(messagePredicate))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        if (filteredData.isEmpty()) {
            return null;
        } else {
            return new RecordedBundle(this.ntpTime, filteredData);
        }
    }
}
