package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import uk.co.mysterymayhem.vmcplayback.osc.OscPlayer;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedMessage;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedPacket;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OscPlayer that replaces input VMC timing messages with its own VMC timing messages that continue to increment over
 * time.
 * <p>
 * Created by Mysteryem on 06/08/2021.
 */
public class VmcPlayer extends OscPlayer {
    public static final String VMC_TIMING_ADDRESS = "/VMC/Ext/T";

    private static final Function<RecordedMessage, RecordedMessage> TIMING_MESSAGE_MAPPER =
            recordedMessage -> {
                if (recordedMessage.getAddress().equals(VMC_TIMING_ADDRESS)) {
                    return VmcTimingMessage.SINGLETON;
                } else {
                    return recordedMessage;
                }
            };

    public VmcPlayer(int portOut, List<RecordedPacket<?, ?>> recordedMessages, long repeatPeriodMillis) throws IOException {
        super(portOut, replaceTimingMessages(recordedMessages), repeatPeriodMillis);
    }

    public VmcPlayer(int portOut, List<RecordedPacket<?, ?>> recordedMessages) throws IOException {
        super(portOut, replaceTimingMessages(recordedMessages));
    }

    public VmcPlayer(SocketAddress socketAddress, List<RecordedPacket<?, ?>> recordedMessages, long repeatPeriodMillis) throws IOException {
        super(socketAddress, replaceTimingMessages(recordedMessages), repeatPeriodMillis);
    }

    public VmcPlayer(SocketAddress socketAddress, List<RecordedPacket<?, ?>> recordedMessages) throws IOException {
        super(socketAddress, replaceTimingMessages(recordedMessages));
    }

    private static List<RecordedPacket<?, ?>> replaceTimingMessages(List<RecordedPacket<?, ?>> inputMessages) {
        return inputMessages.stream()
                .map(recordedPacket -> recordedPacket.mapMessages(TIMING_MESSAGE_MAPPER))
                .collect(Collectors.toList());
    }
}
