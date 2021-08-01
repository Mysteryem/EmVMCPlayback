package uk.co.mysterymayhem.osc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long offsetTime;
    private final String address;
    private final List<Object> arguments;
    private final CharSequence argumentTypes;

    public RecordedMessage(long offsetTime, String address, List<Object> arguments, CharSequence argumentTypes) {
        if (!(argumentTypes instanceof Serializable)) {
            argumentTypes = argumentTypes.toString();
        }
        if (arguments.getClass() != ArrayList.class) {
            arguments = new ArrayList<>(arguments);
        }
        this.offsetTime = offsetTime;
        this.address = address;
        this.arguments = arguments;
        this.argumentTypes = argumentTypes;
    }

    public long getOffsetTime() {
        return offsetTime;
    }

    public String getAddress() {
        return address;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public CharSequence getArgumentTypes() {
        return argumentTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordedMessage that = (RecordedMessage) o;
        return offsetTime == that.offsetTime &&
                Objects.equals(address, that.address) &&
                Objects.equals(arguments, that.arguments) &&
                Objects.equals(argumentTypes, that.argumentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offsetTime, address, arguments, argumentTypes);
    }

    @Override
    public String toString() {
        return "RecordedMessage{" +
                "offsetTime=" + offsetTime +
                ", address='" + address + '\'' +
                ", arguments=" + arguments +
                ", argumentTypes=" + argumentTypes +
                '}';
    }
}
