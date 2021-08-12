package uk.co.mysterymayhem.vmcplayback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.mysterymayhem.vmcplayback.osc.OscPlayer;
import uk.co.mysterymayhem.vmcplayback.osc.OscRecorder;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedPacket;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedPacketData;
import uk.co.mysterymayhem.vmcplayback.osc.vmc.VmcPlayer;
import uk.co.mysterymayhem.vmcplayback.osc.vmc.VmcUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Main class. Handles the program arguments and calls the correct methods based on those arguments.
 * <p>
 * Created by Mysteryem on 31/07/2021.
 */
public class EmVmcPlayback {

    private static final Logger LOG = LoggerFactory.getLogger(EmVmcPlayback.class);

    // Valued arguments
    private static final String[] ARGUMENT_PORT = {"port"};
    private static final String[] ARGUMENT_PORT_IN = {"portin"};
    private static final String[] ARGUMENT_PORT_OUT = {"portout"};
    private static final String[] ARGUMENT_FILE_NAME = {"file"};
    private static final String[] ARGUMENT_RECORDING_DURATION = {"duration"};
    private static final String[] ARGUMENT_MARIONETTE_ADDRESS = {"address"};
    // Flags
    // Replace VMC timing messages when playing back recordings
    private static final String[] FLAG_REPLACE_VMC_TIMING = {"t", "replacevmctiming"};
    // Unfilter message recording/playback to contain all OSC messages instead of only VMC messages
    private static final String[] FLAG_ALLOW_ALL_OSC = {"o", "osc", "all"};

    private String fileName;
    private int portIn;
    private int portOut;
    private int recordingDurationSeconds;
    // 'marionette' is VMC terminology for the receiver of motion data.
    private String marionetteAddress;
    private boolean allowAllOsc;

    public EmVmcPlayback(int portIn, int recordingDurationSeconds, String fileName, boolean allowAllOsc) {
        this.portIn = portIn;
        this.recordingDurationSeconds = recordingDurationSeconds;
        this.fileName = fileName;
        this.allowAllOsc = allowAllOsc;
    }

    public EmVmcPlayback(String fileName, int portOut, String marionetteAddress, boolean allowAllOsc) {
        this.fileName = fileName;
        this.portOut = portOut;
        this.marionetteAddress = marionetteAddress;
        this.allowAllOsc = allowAllOsc;
    }

    public EmVmcPlayback(int portIn, int portOut, int recordingDurationSeconds, String marionetteAddress, boolean allowAllOsc) {
        this.portIn = portIn;
        this.portOut = portOut;
        this.recordingDurationSeconds = recordingDurationSeconds;
        this.marionetteAddress = marionetteAddress;
        this.allowAllOsc = allowAllOsc;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing arguments");
        }
        Map<String, String> arguments = parseArguments(Arrays.copyOfRange(args, 1, args.length));
        try {
            switch (args[0].toLowerCase()) {
                case "record":
                    EmVmcPlayback.recordToFile(arguments);
                    break;
                case "play":
                    EmVmcPlayback.playFromFile(arguments);
                    break;
                case "inout":
                    EmVmcPlayback.recordAndPlayback(arguments);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognised argument '" + args[0] + '"');
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Maybe in the future we could record to file, appending each OSC message as received (or batching a few together
    // at a time) instead of writing the entire file at the end
    // TODO: Replace all these static functions with instance methods
    private static void recordToFile(Map<String, String> arguments) throws IOException, InterruptedException {
        String fileName = removeArgument(arguments, ARGUMENT_FILE_NAME);
        int portIn = Integer.parseInt(removeArgument(arguments, ARGUMENT_PORT));
        int recordingTimeSeconds = Integer.parseInt(removeArgument(arguments, ARGUMENT_RECORDING_DURATION));
        boolean allowAllOsc = removeFlagArgument(arguments, FLAG_ALLOW_ALL_OSC);
        logUnknownArguments(arguments);

        EmVmcPlayback emVmcPlayback = new EmVmcPlayback(portIn, recordingTimeSeconds, fileName, allowAllOsc);

        LOG.info("Recording {} on port {} to {} for {}s will start in:", allowAllOsc ? "all OSC messages" : "VMC messages", portIn, fileName, recordingTimeSeconds);
        EmVmcPlayback.recordingCountdown();
        List<RecordedPacket<?, ?>> recordedMessages = emVmcPlayback.record();

        emVmcPlayback.saveToFile(recordedMessages);
    }

    private static void playFromFile(Map<String, String> arguments) throws IOException {
        String fileName = removeArgument(arguments, ARGUMENT_FILE_NAME);
        int portOut = Integer.parseInt(removeArgument(arguments, ARGUMENT_PORT));
        String marionetteAddress = removeArgument(arguments, "localhost", ARGUMENT_MARIONETTE_ADDRESS);
        boolean replaceVmcTime = removeFlagArgument(arguments, FLAG_REPLACE_VMC_TIMING);
        boolean allowAllOsc = removeFlagArgument(arguments, FLAG_ALLOW_ALL_OSC);
        logUnknownArguments(arguments);

        EmVmcPlayback emVmcPlayback = new EmVmcPlayback(fileName, portOut, marionetteAddress, allowAllOsc);

        List<RecordedPacket<?, ?>> recordedPackets = emVmcPlayback.loadFromFile();

        // The packets didn't just get recorded so the messages need to be counted manually
        int messageCount = recordedPackets.stream()
                // Gives "java.lang.BootstrapMethodError: call site initialization exception" if I leave out the
                // explicit cast for some reason. Compiler error? Even IntelliJ thinks the return type should be Object,
                // resulting in what would be a Stream<?>
                // I wonder if '? extends RecordedPacketData<?>' is technically more accurate for the explicit cast
                .map((Function<RecordedPacket<?, ?>, RecordedPacketData<?>>) RecordedPacket::getPacketData)
                .mapToInt(RecordedPacketData::getMessageCount)
                .sum();

        OscPlayer oscPlayer = emVmcPlayback.startPlayback(recordedPackets, replaceVmcTime);

        LOG.info("Started looping playback of {} from '{}' ({} packets and {} messages total) to {}:{}. " +
                        "VMC timing message replacement is: {}",
                allowAllOsc ? "all OSC messages" : "only VMC messages",
                fileName, recordedPackets.size(), messageCount, marionetteAddress, portOut,
                replaceVmcTime ? "enabled" : "disabled");

        EmVmcPlayback.stopPlaybackOnUserInput(oscPlayer);
    }

    private static void recordAndPlayback(Map<String, String> arguments) throws IOException, InterruptedException {
        int portIn = Integer.parseInt(removeArgument(arguments, ARGUMENT_PORT_IN));
        int portOut = Integer.parseInt(removeArgument(arguments, ARGUMENT_PORT_OUT));
        int recordingTimeSeconds = Integer.parseInt(removeArgument(arguments, ARGUMENT_RECORDING_DURATION));
        String marionetteAddress = removeArgument(arguments, "localhost", ARGUMENT_MARIONETTE_ADDRESS);
        boolean replaceVmcTime = removeFlagArgument(arguments, FLAG_REPLACE_VMC_TIMING);
        boolean allowAllOsc = removeFlagArgument(arguments, FLAG_ALLOW_ALL_OSC);
        logUnknownArguments(arguments);

        EmVmcPlayback emVmcPlayback = new EmVmcPlayback(portIn, portOut, recordingTimeSeconds, marionetteAddress, allowAllOsc);

        LOG.info("Temporary recording of {} on port {} for {}s will start in:", allowAllOsc ? "all OSC messages" : "VMC messages", portIn, recordingTimeSeconds);
        EmVmcPlayback.recordingCountdown();

        List<RecordedPacket<?, ?>> recordedPackets = emVmcPlayback.record();

        OscPlayer oscPlayer = emVmcPlayback.startPlayback(recordedPackets, replaceVmcTime);

        LOG.info("Started looping playback of recorded {}s to {}:{}. VMC timing message replacement is: {}.", recordingTimeSeconds, marionetteAddress, portOut, replaceVmcTime ? "enabled" : "disabled");

        EmVmcPlayback.stopPlaybackOnUserInput(oscPlayer);
    }

    private static void stopPlaybackOnUserInput(OscPlayer player) {
        // Keep running until user presses enter
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to quit");
        if (scanner.hasNextLine()) {
            player.stop();
        }
    }

    private static void recordingCountdown() throws InterruptedException {
        System.out.println("3");
        Thread.sleep(1000);
        System.out.println("2");
        Thread.sleep(1000);
        System.out.println("1");
        Thread.sleep(1000);
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> argumentsMap = new HashMap<>();
        for (String arg : args) {
            // All arguments start with -
            if (arg.startsWith("-")) {
                // Argument could have a value or could be a non-combining flag
                // e.g. '--myArg=Value' or '--myFlag'
                if (arg.startsWith("--")) {
                    // strip off all preceding '-'
                    arg = arg.replaceFirst("^-+", "");

                    // Look for an equals symbol, search from the start of the string
                    int firstEqualsIndex = arg.indexOf('=');
                    if (firstEqualsIndex != -1) {
                        // If there's an equals symbol, there should be a value after it
                        String value = arg.substring(firstEqualsIndex + 1);
                        // Get the part before the '=' and convert to lowercase
                        arg = arg.substring(0, firstEqualsIndex).toLowerCase();
                        argumentsMap.put(arg, value);
                    } else {
                        // There's no equals sign so it must be a flag
                        argumentsMap.put(arg, "true");
                    }
                }
                // Argument can't have a value, it is a single character flag or could be multiple single character
                // flags combined together
                // e.g. '-e', '-rt' or '-kmo'
                else {
                    // Convert to char array and ignore the preceding '-'
                    char[] flags = arg.substring(1).toCharArray();
                    for (char flag : flags) {
                        argumentsMap.put(Character.toString(flag), "true");
                    }
                }
            } else {
                LOG.warn("Invalid argument '{}'", arg);
            }
        }
        return argumentsMap;
    }

    private static String removeArgument(Map<String, String> map, String... argumentNames) {
        return removeArgument(map, null, argumentNames);
    }

    private static String removeArgument(Map<String, String> map, String defaultValue, String... argumentNames) {
        for (String argumentName : argumentNames) {
            if (map.containsKey(argumentName)) {
                return map.remove(argumentName);
            }
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException("No suitable argument found matching " + Arrays.toString(argumentNames));
        } else {
            return defaultValue;
        }
    }

    private static boolean removeFlagArgument(Map<String, ?> map, String... flagNames) {
        for (String flagName : flagNames) {
            if (map.remove(flagName) != null) {
                return true;
            }
        }
        return false;
    }

    private static void logUnknownArguments(Map<String, String> map) {
        map.forEach((k, v) -> LOG.warn("Unrecognised argument name '{}' with value '{}'", k, v));
    }

    @SuppressWarnings("unchecked")
    private List<RecordedPacket<?, ?>> loadFromFile() throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(Paths.get(this.fileName)));
        ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
        try {
            List<RecordedPacket<?, ?>> recordedMessages = (List<RecordedPacket<?, ?>>) objectInputStream.readObject();
            objectInputStream.close();
            gzipInputStream.close();
            if (!this.allowAllOsc) {
                recordedMessages = recordedMessages.stream()
                        .map(recordedPacket -> recordedPacket.filter(VmcUtils::isVmc))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return recordedMessages;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<RecordedPacket<?, ?>> record() throws IOException {
        OscRecorder oscRecorder;
        if (this.allowAllOsc) {
            oscRecorder = new OscRecorder(m -> true, this.portIn);
        } else {
            oscRecorder = new OscRecorder(VmcUtils::isVmc, this.portIn);
        }
        oscRecorder.init();

        LOG.info("Recording");
        oscRecorder.startRecording();
        // wait for messages for the input number of seconds
        try {
            Thread.sleep(this.recordingDurationSeconds * 1000);
        } catch (InterruptedException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        List<RecordedPacket<?, ?>> recordedMessages = oscRecorder.stopRecording();
        LOG.info("Recording stopped");
        LOG.info("Recorded {} packets ({} messages total) in {} milliseconds", oscRecorder.getPacketCount(), oscRecorder.countMessages(), oscRecorder.getRecordingDurationMillis());
        return recordedMessages;
    }

    private void saveToFile(List<RecordedPacket<?, ?>> recordedPackets) throws IOException {
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(Paths.get(this.fileName)));
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);
        objectOutputStream.writeObject(recordedPackets);
        objectOutputStream.flush();
        objectOutputStream.close();
        gzipOutputStream.flush();
        gzipOutputStream.close();
    }

    private OscPlayer startPlayback(List<RecordedPacket<?, ?>> recordedMessages, boolean replaceVmcTimingMessages) throws IOException {

        OscPlayer oscPlayer;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(this.marionetteAddress, this.portOut);
        if (replaceVmcTimingMessages) {
            oscPlayer = new VmcPlayer(inetSocketAddress, recordedMessages);
        } else {
            oscPlayer = new OscPlayer(inetSocketAddress, recordedMessages);
        }

        oscPlayer.start();
        return oscPlayer;
    }

}
