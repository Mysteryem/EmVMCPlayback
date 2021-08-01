package uk.co.mysterymayhem.osc.vmc;

import uk.co.mysterymayhem.osc.OscRecorder;

import java.io.IOException;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class VmcRecorder extends OscRecorder {

    public VmcRecorder(int port) throws IOException {
        super(new VmcMessageSelector(), port);
    }
}