package uk.co.mysterymayhem.vmcplayback.osc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageInfo;
import com.illposed.osc.OSCPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents the data of a recorded OSC message.
 * <p>
 * Created by Mysteryem on 31/07/2021.
 */
public class RecordedMessage implements RecordedPacketData<RecordedMessage>, Serializable {
    private static final long serialVersionUID = 3L;

    private final String address;
    transient private List<Object> arguments;
    transient private CharSequence argumentTypes;

    public RecordedMessage(OSCMessage oscMessage) {
        this(oscMessage.getAddress(), oscMessage.getArguments(), oscMessage.getInfo().getArgumentTypeTags());
    }

    public RecordedMessage(String address, List<Object> arguments, CharSequence argumentTypes) {
        if (!(argumentTypes instanceof Serializable)) {
            argumentTypes = argumentTypes.toString();
        }
        if (arguments.getClass() != ArrayList.class) {
            arguments = new ArrayList<>(arguments);
        }
        this.address = address;
        this.arguments = arguments;
        this.argumentTypes = argumentTypes;
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
    public int getMessageCount() {
        return 1;
    }

    @Override
    public OSCPacket toOscPacket() {
        OSCMessageInfo oscMessageInfo = new OSCMessageInfo(this.getArgumentTypes());
        return new OSCMessage(this.getAddress(), this.getArguments(), oscMessageInfo);
    }

    @Override
    public RecordedMessage filter(Predicate<RecordedMessage> messagePredicate) {
        if (messagePredicate.test(this)) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public RecordedMessage mapMessages(Function<RecordedMessage, RecordedMessage> mapper) {
        return mapper.apply(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordedMessage that = (RecordedMessage) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(arguments, that.arguments) &&
                Objects.equals(argumentTypes, that.argumentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, arguments, argumentTypes);
    }

    @Override
    public String toString() {
        return "RecordedMessage{" +
                "address='" + address + '\'' +
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

        Object[] argumentsArray = (Object[]) ois.readObject();
        String argumentTypes = (String) ois.readObject();

        this.arguments = Arrays.asList(argumentsArray);
        this.argumentTypes = argumentTypes;
    }


}
