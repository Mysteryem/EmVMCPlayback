package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import com.illposed.osc.OSCPacket;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.Collections;

/**
 * Replacement message for recorded VMC timing messages that calculates time when sent
 * <p>
 * Created by Mysteryem on 25/08/2021.
 */
public final class VmcTimingMessage extends RecordedMessage {
    public static final VmcTimingMessage SINGLETON = new VmcTimingMessage();
    private static final long serialVersionUID = 1L;

    private VmcTimingMessage() {
        super(VmcPlayer.VMC_TIMING_ADDRESS, Collections.emptyList(), "f");
    }

    @Override
    public OSCPacket toOscPacket() {
        return VmcTimingOSCMessage.SINGLETON;
    }


    // Since this class extends a Serializable class, though this class probably shouldn't need to be
    // serialized ever
    private void writeObject(ObjectOutputStream out) throws IOException {
        //do nothing
    }

    // Since this class extends a Serializable class, though this class probably shouldn't need to be
    // serialized ever
    protected Object readResolve() throws ObjectStreamException {
        return VmcTimingMessage.SINGLETON;
    }

    @Override
    public String toString() {
        return "VmcTimingMessage{} " + super.toString();
    }
}
