package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.util.TimerTask;

/**
 * Task that can be added to a Timer in order to play back a recorded OSC packet.
 * <p>
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordedPacketTimerTask extends TimerTask {

    private final OSCPortOut portOut;
    private final OSCPacket oscPacket;
    private final long delayMillis;

    RecordedPacketTimerTask(RecordedPacket<?, ?> recordedPacket, OSCPortOut portOut) {
        this.portOut = portOut;
        this.delayMillis = recordedPacket.getOffsetTime();
        this.oscPacket = recordedPacket.toOscPacket();
    }

    @Override
    public void run() {
        try {
            portOut.send(this.oscPacket);
        } catch (IOException | OSCSerializeException e) {
            throw new RuntimeException(e);
        }
    }

    public long getDelayMillis() {
        return this.delayMillis;
    }
}
