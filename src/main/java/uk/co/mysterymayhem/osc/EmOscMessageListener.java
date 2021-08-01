package uk.co.mysterymayhem.osc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageEvent;
import com.illposed.osc.OSCMessageListener;
import com.illposed.osc.transport.udp.OSCPortOut;

import java.util.ArrayList;
import java.util.Timer;

/**
 * Created by Mysteryem on 31/07/2021.
 */
public class EmOscMessageListener implements OSCMessageListener {

    private int messageCount;
    private long startTime;
    private ArrayList<RecordedMessage> recordedMessages = new ArrayList<>();

    @Override
    public void acceptMessage(OSCMessageEvent event) {
        long timeReceived = System.currentTimeMillis();
        messageCount++;
        long offsetTime = timeReceived - this.startTime;
        OSCMessage message = event.getMessage();

        RecordedMessage recordedMessage = new RecordedMessage(offsetTime, message.getAddress(), message.getArguments(), message.getInfo().getArgumentTypeTags());
        this.recordedMessages.add(recordedMessage);
    }

    public void startRecording() {
        // ditch any old messages
        this.recordedMessages = new ArrayList<>();
        // get the time now for use when calculating time offsets of when messages have been received
        this.startTime = System.currentTimeMillis();
    }

    public void scheduleTimerTasks(Timer timer, OSCPortOut portOut, long repetitionPeriod) {
        this.recordedMessages.forEach(recordedMessage -> {
            RecordedMessageTimerTask recordedTimerTask = new RecordedMessageTimerTask(recordedMessage, portOut);
            timer.scheduleAtFixedRate(recordedTimerTask, recordedMessage.getOffsetTime(), repetitionPeriod);
        });
    }

    public int getMessageCount() {
        return messageCount;
    }

    public long getStartTime() {
        return startTime;
    }

    public ArrayList<RecordedMessage> getRecordedMessages() {
        return recordedMessages;
    }

}
