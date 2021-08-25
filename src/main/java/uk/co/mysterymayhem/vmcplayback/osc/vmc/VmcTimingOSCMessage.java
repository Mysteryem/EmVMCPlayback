package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageInfo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.Collections;
import java.util.List;

/**
 * OSCMessage for VMC timing messages that calculates the time to include in the OSC message when it's sent (whenever
 * getArguments() is called)
 * <p>
 * Created by Mysteryem on 25/08/2021.
 */
public final class VmcTimingOSCMessage extends OSCMessage {
    public static final VmcTimingOSCMessage SINGLETON = new VmcTimingOSCMessage();
    private static final long serialVersionUID = 1L;
    private final long startTimeNano = System.nanoTime();

    private VmcTimingOSCMessage() {
        super(VmcPlayer.VMC_TIMING_ADDRESS, Collections.emptyList(), new OSCMessageInfo("f"));
    }

    @Override
    public List<Object> getArguments() {
        return Collections.singletonList(currentElapsedTime());
    }

    private float currentElapsedTime() {
        // Nanoseconds to seconds as float
        return (System.nanoTime() - this.startTimeNano) / 1.0E09f;
    }

    // Since this class extends a Serializable class, though this class probably shouldn't need to be
    // serialized ever
    private void writeObject(ObjectOutputStream out) throws IOException {
        //do nothing
    }

    // Since this class extends a Serializable class, though this class probably shouldn't need to be
    // serialized ever
    protected Object readResolve() throws ObjectStreamException {
        return VmcTimingOSCMessage.SINGLETON;
    }
}
