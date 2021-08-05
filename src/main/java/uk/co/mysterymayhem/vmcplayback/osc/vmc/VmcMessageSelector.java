package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessageEvent;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class VmcMessageSelector implements MessageSelector {

    @Override
    public boolean isInfoRequired() {
        // Not sure if required, the extra info is usually the parameter types only
        return false;
    }

    @Override
    public boolean matches(OSCMessageEvent messageEvent) {
        return messageEvent.getMessage().getAddress().startsWith("/VMC");
    }
}
