package uk.co.mysterymayhem.vmcplayback.osc;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordedMessage implements Serializable {
    private static final long serialVersionUID = 2L;

    private final long offsetTime;
    private final String address;
    transient private List<Object> arguments;
    transient private CharSequence argumentTypes;

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

    // There will likely be hundreds of thousands of these instances serialized so optimising is a good idea
    // So make sure we're not serializing some weird List or CharSequence subclasses that take up extra filesize
    // TODO: See if serializing a mapping from address to an addressId alongside the RecordedMessages saves space (when taking into account the gz compression)
    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();

        Object[] argumentsArray = this.getArguments().toArray();
        CharSequence argumentTypes = this.getArgumentTypes();
        if (argumentTypes.getClass() != String.class) {
            argumentTypes = argumentTypes.toString();
        }

        oos.writeObject(argumentsArray);
        oos.writeObject(argumentTypes);
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();

        Object[] argumentsArray = (Object[])ois.readObject();
        String argumentTypes = (String)ois.readObject();

        this.arguments = Arrays.asList(argumentsArray);
        this.argumentTypes = argumentTypes;
    }


}
