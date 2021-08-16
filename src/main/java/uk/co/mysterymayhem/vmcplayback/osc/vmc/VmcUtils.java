package uk.co.mysterymayhem.vmcplayback.osc.vmc;

import uk.co.mysterymayhem.vmcplayback.osc.RecordedMessage;

import java.util.List;
import java.util.function.Predicate;

/**
 * Miscellaneous VMC utilities that don't have a better home
 * <p>
 * Created by Mysteryem on 06/08/2021.
 */
public class VmcUtils {
    // All of the output from VSeeFace is "/VMC/Ext/[...]", is there something that doesn't have the "Ext"?
    public static final String VMC_ADDRESS_PREFIX = "/VMC";

    // Root position/rotation
    public static final String VMC_ROOT_ADDRESS = "/VMC/Ext/Root/Pos";
    // Bone position/rotation
    public static final String VMC_BONE_ADDRESS = "/VMC/Ext/Bone/Pos";
    // Tracking? position/rotation
    public static final String VMC_TRA_ADDRESS = "/VMC/Ext/Tra/Pos";

    public static final String LEFT_EYE_BONE_NAME = "LeftEye";
    public static final String RIGHT_EYE_BONE_NAME = "RightEye";

    public static final Predicate<RecordedMessage> FILTER_OUT_BODY_AND_HEAD_MOVEMENT = rm -> {
        String address = rm.getAddress();
        switch (address) {
            case VMC_BONE_ADDRESS:
                List<Object> arguments = rm.getArguments();
                if (arguments.size() >= 1) {
                    Object arg0 = arguments.get(0);
                    return LEFT_EYE_BONE_NAME.equals(arg0) || RIGHT_EYE_BONE_NAME.equals(arg0);
                }
            case VMC_ROOT_ADDRESS:
            case VMC_TRA_ADDRESS:
                return false;
            default:
                return true;
        }
    };

    public static boolean isVmc(RecordedMessage recordedMessage) {
        return isVmc(recordedMessage.getAddress());
    }

    public static boolean isVmc(String oscAddress) {
        return oscAddress.startsWith(VMC_ADDRESS_PREFIX);
    }


    /**
     * Build a message filtering Predicate
     *
     * @param allowAllOsc               true if all OSC messages are allowed, false if only VMC messages are allowed
     * @param filterBodyAndHeadTracking true if VMC messages for bone movement (excluding eyes) should be filtered out
     * @return A predicate matching the chosen options, or null if no filtering is required
     */
    // If we make many more filters, swap to using a method that returns a FilterBuilder instance, for now, it's easier
    // to have this method, with an argument per filter, that does all the building
    public static Predicate<RecordedMessage> buildMessageFilter(boolean allowAllOsc, boolean filterBodyAndHeadTracking) {
        FilterBuilder filterBuilder = new FilterBuilder();
        if (!allowAllOsc) {
            filterBuilder.vmcOnly();
        }
        if (filterBodyAndHeadTracking) {
            filterBuilder.filterBodyAndHeadTracking();
        }
        return filterBuilder.build();
    }

    public static class FilterBuilder {
        private Predicate<RecordedMessage> filter = null;

        FilterBuilder() {
        }

        private FilterBuilder vmcOnly() {
            return this.and(VmcUtils::isVmc);
        }

        private FilterBuilder filterBodyAndHeadTracking() {
            return this.and(FILTER_OUT_BODY_AND_HEAD_MOVEMENT);
        }

        private FilterBuilder and(Predicate<RecordedMessage> predicate) {
            if (filter == null) {
                filter = predicate;
            } else {
                filter = filter.and(predicate);
            }
            return this;
        }

        Predicate<RecordedMessage> build() {
            return this.filter;
        }
    }
}
