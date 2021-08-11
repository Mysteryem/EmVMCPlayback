package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessageEvent;

import java.util.function.Predicate;

/**
 * Created by Mysteryem on 09/08/2021.
 */
public class GenericMessageSelector implements MessageSelector {
    private final boolean infoRequired;
    private final Predicate<? super OSCMessageEvent> matcher;

    public GenericMessageSelector(boolean infoRequired, Predicate<? super OSCMessageEvent> matcher) {
        this.infoRequired = infoRequired;
        this.matcher = matcher;
    }

    @Override
    public boolean isInfoRequired() {
        return this.infoRequired;
    }

    @Override
    public boolean matches(OSCMessageEvent messageEvent) {
        return this.matcher.test(messageEvent);
    }
}
