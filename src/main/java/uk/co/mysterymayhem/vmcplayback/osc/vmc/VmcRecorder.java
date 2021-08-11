package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import uk.co.mysterymayhem.vmcplayback.osc.GenericMessageSelector;
import uk.co.mysterymayhem.vmcplayback.osc.OscRecorder;

import java.io.IOException;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class VmcRecorder extends OscRecorder {
    public VmcRecorder(int port) throws IOException {
        super(new GenericMessageSelector(false, VmcUtils::isVmc), port);
    }
}
