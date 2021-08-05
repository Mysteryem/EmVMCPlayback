package uk.co.mysterymayhem.vmcplayback;

import uk.co.mysterymayhem.vmcplayback.osc.OscPlayer;
import uk.co.mysterymayhem.vmcplayback.osc.RecordedMessage;
import uk.co.mysterymayhem.vmcplayback.osc.vmc.VmcRecorder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class EmOSC {
    private String fileName;
    private int portIn;
    private int portOut;
    private int recordingDurationSeconds;
    // 'marionette' is VMC terminology
    // Currently unused as "localhost" is being assumed
    private String marionetteAddress;

    public EmOSC(int portIn, int recordingDurationSeconds, String fileName) {
        this.portIn = portIn;
        this.recordingDurationSeconds = recordingDurationSeconds;
        this.fileName = fileName;
    }

    public EmOSC(String fileName, int portOut, String marionetteAddress) {
        this.fileName = fileName;
        this.portOut = portOut;
        this.marionetteAddress = marionetteAddress;
    }

    public EmOSC(int portIn, int portOut, int recordingDurationSeconds, String marionetteAddress) {
        this.portIn = portIn;
        this.portOut = portOut;
        this.recordingDurationSeconds = recordingDurationSeconds;
        this.marionetteAddress = marionetteAddress;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing arguments");
        }
        try {
            switch (args[0].toLowerCase()) {
                case "record":
                    EmOSC.recordToFile(args);
                    break;
                case "play":
                    EmOSC.playFromFile(args);
                    break;
                case "inout":
                    EmOSC.recordAndPlayback(args);
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
    private static void recordToFile(String[] args) throws IOException, InterruptedException {
        String fileName = args[1];
        int portIn = Integer.parseInt(args[2]);
        int recordingTimeSeconds = Integer.parseInt(args[3]);

        EmOSC emOSC = new EmOSC(portIn, recordingTimeSeconds, fileName);

        EmOSC.recordingCountdown();
        List<RecordedMessage> recordedMessages = emOSC.record();

        emOSC.saveToFile(recordedMessages);
    }

    private static void playFromFile(String[] args) throws IOException {
        String fileName = args[1];
        int portOut = Integer.parseInt(args[2]);
        String marionetteAddress = args.length > 3 ? args[3] : "localhost";

        EmOSC emOSC = new EmOSC(fileName, portOut, marionetteAddress);

        List<RecordedMessage> recordedMessages = emOSC.loadFromFile();

        OscPlayer oscPlayer = emOSC.startPlayback(recordedMessages);

        EmOSC.stopPlaybackOnUserInput(oscPlayer);
    }

    private static void recordAndPlayback(String[] args) throws IOException, InterruptedException {
        int portIn = Integer.parseInt(args[1]);
        int portOut = Integer.parseInt(args[2]);
        int recordingTimeSeconds = Integer.parseInt(args[3]);
        String marionetteAddress = args.length > 4 ? args[4] : "localhost";

        EmOSC emOSC = new EmOSC(portIn, portOut, recordingTimeSeconds, marionetteAddress);

        EmOSC.recordingCountdown();

        List<RecordedMessage> recordedMessages = emOSC.record();

        OscPlayer oscPlayer = emOSC.startPlayback(recordedMessages);

        EmOSC.stopPlaybackOnUserInput(oscPlayer);
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
        System.out.println("Recording will start in");
        System.out.println("3");
        Thread.sleep(1000);
        System.out.println("2");
        Thread.sleep(1000);
        System.out.println("1");
        Thread.sleep(1000);
    }

    @SuppressWarnings("unchecked")
    private List<RecordedMessage> loadFromFile() throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(Paths.get(this.fileName)));
        ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
        try {
            List<RecordedMessage> recordedMessages = (List<RecordedMessage>) objectInputStream.readObject();
            objectInputStream.close();
            gzipInputStream.close();
            return recordedMessages;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<RecordedMessage> record() throws IOException {
        VmcRecorder vmcRecorder = new VmcRecorder(this.portIn);
        vmcRecorder.init();

        System.out.println("Recording");
        vmcRecorder.startRecording();
        // wait for messages for the input number of seconds
        try {
            Thread.sleep(this.recordingDurationSeconds * 1000);
        } catch (InterruptedException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        List<RecordedMessage> recordedMessages = vmcRecorder.stopRecording();
        System.out.println("Recording stopped");
        System.out.println("Recorded " + vmcRecorder.getMessageCount() + " messages in " + vmcRecorder.getRecordingDurationMillis() + " milliseconds");
        return recordedMessages;
    }

    private void saveToFile(List<RecordedMessage> recordedMessages) throws IOException {
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(Paths.get(this.fileName)));
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);
        objectOutputStream.writeObject(recordedMessages);
        objectOutputStream.flush();
        objectOutputStream.close();
        gzipOutputStream.flush();
        gzipOutputStream.close();
    }

    private OscPlayer startPlayback(List<RecordedMessage> recordedMessages) throws IOException {

        OscPlayer oscPlayer = new OscPlayer(this.portOut, recordedMessages);

        oscPlayer.start();
        System.out.println("Started playback");
        return oscPlayer;
    }

}
