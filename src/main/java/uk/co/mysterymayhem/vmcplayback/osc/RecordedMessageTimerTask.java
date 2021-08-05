package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageInfo;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.util.TimerTask;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordedMessageTimerTask extends TimerTask {

    private final RecordedMessage recordedMessage;
    private final OSCPortOut portOut;

    RecordedMessageTimerTask(RecordedMessage recordedMessage, OSCPortOut portOut) {
        this.recordedMessage = recordedMessage;
        this.portOut = portOut;
    }

    @Override
    public void run() {
        OSCMessageInfo oscMessageInfo = new OSCMessageInfo(recordedMessage.getArgumentTypes());
        OSCMessage message = new OSCMessage(recordedMessage.getAddress(), recordedMessage.getArguments(), oscMessageInfo);
        try {
            portOut.send(message);
        } catch (IOException | OSCSerializeException e) {
            throw new RuntimeException(e);
        }
    }

    public long getDelayMillis() {
        return this.recordedMessage.getOffsetTime();
    }
}
