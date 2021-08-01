package uk.co.mysterymayhem.osc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.co.mysterymayhem.osc.vmc.VmcRecorder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class EmOSC {

    public static void main(String[] args) {
        try {
            switch (args[0].toLowerCase()) {
                case "record":
                    recordToFile(args);
                    break;
                case "play":
                    playFromFile(args);
                    break;
                case "inout":
                    recordAndPlayback(args);
                    break;
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

        recordingCountdown();
        saveToFile(record(portIn, recordingTimeSeconds), fileName);
    }

    private static void playFromFile(String[] args) throws IOException {
        String fileName = args[1];
        int portOut = Integer.parseInt(args[2]);
        String marionetteAddress = args.length > 3 ? args[3] : "localhost";

        playback(loadFromFile(fileName), portOut, marionetteAddress);
    }

    private static void recordAndPlayback(String[] args) throws IOException, InterruptedException {
        int portIn = Integer.parseInt(args[1]);
        int portOut = Integer.parseInt(args[2]);
        int recordingTimeSeconds = Integer.parseInt(args[3]);
        String marionetteAddress = args.length > 4 ? args[4] : "localhost";
        recordingCountdown();
        playback(record(portIn, recordingTimeSeconds), portOut, marionetteAddress);
    }

    private static List<RecordedMessage> loadFromFile(String file) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(Paths.get(file)));
        //TODO: Get rid of the whole JSON side of things entirely, some binary file format is all that's needed, maybe even using Java's Serializable?
        ObjectMapper objectMapper = new ObjectMapper();
        List<RecordedMessage> recordedMessages = objectMapper.readValue(gzipInputStream, new TypeReference<List<RecordedMessage>>() {
        });
        for (RecordedMessage r : recordedMessages) {
            List<Object> arguments = r.getArguments();
            ArrayList<Object> fixedArguments = new ArrayList<>();
            for (Object o : arguments) {
                if (o instanceof Double) {
                    fixedArguments.add(((Double) o).floatValue());
                } else {
                    fixedArguments.add(o);
                }
            }
            r.setArguments(fixedArguments);
        }
        gzipInputStream.close();
        return recordedMessages;
    }

    private static List<RecordedMessage> record(int portIn, int recordingTimeSeconds) throws IOException {
        VmcRecorder vmcRecorder = new VmcRecorder(portIn);
        vmcRecorder.init();

        System.out.println("Recording");
        vmcRecorder.startRecording();
        // wait for messages for the input number of seconds
        try {
            Thread.sleep(recordingTimeSeconds * 1000);
        } catch (InterruptedException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        List<RecordedMessage> recordedMessages = vmcRecorder.stopRecording();
        System.out.println("Recording stopped");
        System.out.println("Recorded " + vmcRecorder.getMessageCount() + " messages in " + vmcRecorder.getRecordingDurationMillis() + " milliseconds");
        return recordedMessages;
    }

    private static void saveToFile(List<RecordedMessage> recordedMessages, String file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] jsonBytes = objectMapper.writeValueAsBytes(recordedMessages);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(Paths.get(file)));
        gzipOutputStream.write(jsonBytes);
        gzipOutputStream.finish();
    }

    private static void playback(List<RecordedMessage> recordedMessages, int portOut, String marionetteAddress) throws IOException {
        OscPlayer oscPlayer = new OscPlayer(portOut, recordedMessages);

        oscPlayer.start();
        System.out.println("Started playback");

        // Keep running until
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to quit");
        if (scanner.hasNextLine()) {
            oscPlayer.stop();
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

}
