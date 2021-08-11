package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import com.illposed.osc.OSCMessageEvent;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedMessage;

/**
 * Created by Mysteryem on 06/08/2021.
 */
public class VmcUtils {
    // All of the output from VSeeFace is "/VMC/Ext/[...]", is there something that doesn't have the "Ext"?
    public static final String VMC_ADDRESS_PREFIX = "/VMC";

    public static boolean isVmc(OSCMessageEvent messageEvent) {
        return isVmc(messageEvent.getMessage().getAddress());
    }

    public static boolean isVmc(RecordedMessage recordedMessage) {
        return isVmc(recordedMessage.getAddress());
    }

    public static boolean isVmc(String oscAddress) {
        return oscAddress.startsWith(VMC_ADDRESS_PREFIX);
    }
}
