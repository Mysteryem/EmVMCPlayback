package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageInfo;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;
import uk.co.mysterymayhem.vmcplayback.osc.OscPlayer;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;

//TODO To maintain timing accuracy, instead of adding in a our own timing message, we could set all of the timing
// messages to increment their value by an amount equal to
// (last timing value) + (average difference between timings) - (first timing value)
// so that the first looped timing value is 1 message in time after the last message of the initial run of messages
// -
// It would be a good idea to check that the standard deviation isn't very high and maybe eliminate any values that
// stray too far from the median, mean, or similar

/**
 * OscPlayer that replaces input VMC timing messages with its own VMC timing messages that continue to increment over
 * time.
 * <p>
 * Created by Mysteryem on 06/08/2021.
 */
public class VmcPlayer extends OscPlayer {
    public static final String VMC_TIMING_ADDRESS = "/VMC/Ext/T";
    // Period for timing messages
    // VSeeFace seems to aim for a period of 60Hz (1/60 seconds), other programs could vary
    // We can't do an accurate repeating of 1/60th of a second since we can only schedule a period of milliseconds
    // 167ms repeating would soon get out of sync of the target value 166.66666666666 recurring, all the tasks would
    // have to be scheduled in advance for the closest millisecond values to 1000/60, 2000/60, 3000/60 etc up to a full
    // second at 60000/60 (though since 3000/60 is a whole number of milliseconds we could do something with repeating
    // in a cycle of 3 tasks
    // --
    // As of VMC V2.3, the update period is set by the Marionette with
    // /VMC/Ext/Set/Period (int){Status} (int){Root} (int){Bone} (int){BlendShape} (int){Camera} (int){Devices}
    // sent to the Performer. Does VSeeFace even send messages to the Performer?
    private static final long TIMING_PERIOD_MILLIS = 100;

    public VmcPlayer(int portOut, List<RecordedMessage> recordedMessages, long repeatPeriodMillis) throws IOException {
        super(portOut, filterTimingMessages(recordedMessages), repeatPeriodMillis);
    }

    public VmcPlayer(int portOut, List<RecordedMessage> recordedMessages) throws IOException {
        super(portOut, filterTimingMessages(recordedMessages));
    }

    public VmcPlayer(SocketAddress socketAddress, List<RecordedMessage> recordedMessages, long repeatPeriodMillis) throws IOException {
        super(socketAddress, filterTimingMessages(recordedMessages), repeatPeriodMillis);
    }

    public VmcPlayer(SocketAddress socketAddress, List<RecordedMessage> recordedMessages) throws IOException {
        super(socketAddress, filterTimingMessages(recordedMessages));
    }

    private static List<RecordedMessage> filterTimingMessages(List<RecordedMessage> inputMessages) {
        return inputMessages.stream()
                .filter(message -> !message.getAddress().equals(VMC_TIMING_ADDRESS))
                .collect(Collectors.toList());
    }

    @Override
    protected void addRecordedMessages() {
        this.scheduleTask(new VmcTimingTimerTask(this.oscPortOut), 0, TIMING_PERIOD_MILLIS);
        super.addRecordedMessages();
    }

    private static class VmcTimingTimerTask extends TimerTask {

        private final OSCPortOut portOut;
        private final OSCMessageInfo oscMessageInfo = new OSCMessageInfo("f");
        private long time = 0;

        VmcTimingTimerTask(OSCPortOut portOut) {
            this.portOut = portOut;
        }

        @Override
        public void run() {
            float time = this.time / 1000f;
            OSCMessage message = new OSCMessage(VMC_TIMING_ADDRESS, Collections.singletonList(time), this.oscMessageInfo);
            this.time += TIMING_PERIOD_MILLIS;
            try {
                portOut.send(message);
            } catch (IOException | OSCSerializeException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
