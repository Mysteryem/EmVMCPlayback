package uk.co.mysterymayhem.osc;

import java.util.List;
import java.util.Objects;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordedMessage {
    private long offsetTime;
    private String address;
    private List<Object> arguments;
    private CharSequence argumentTypes;

    // For json
    private RecordedMessage() {
    }

    public RecordedMessage(long offsetTime, String address, List<Object> arguments, CharSequence argumentTypes) {
        this.offsetTime = offsetTime;
        this.address = address;
        this.arguments = arguments;
        this.argumentTypes = argumentTypes;
    }

    public long getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(long offsetTime) {
        this.offsetTime = offsetTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }

    public CharSequence getArgumentTypes() {
        return argumentTypes;
    }

    public void setArgumentTypes(CharSequence argumentTypes) {
        this.argumentTypes = argumentTypes;
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
