package com.github.athingx.athing.aliyun.nlp.boot;

import com.github.athingx.athing.aliyun.nlp.api.NlpThingCom;
import com.github.athingx.athing.aliyun.nlp.api.RecordHandler;
import com.github.athingx.athing.standard.thing.ThingException;
import com.github.athingx.athing.standard.thing.ThingFuture;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.junit.Test;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class NlpTestCase extends PuppetSupport {

    @Test
    public void speaker() throws ThingException, ExecutionException {

        final Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]);
        tPuppet.getThingCom(NlpThingCom.class, true)
                .getSpeaker(mixer)
                .speak("凌飞是傻B，凌飞是傻B，凌飞是傻B，凌飞是傻B，凌飞是傻B")
                .syncUninterruptible();

    }


    @Test
    public void recorder$recording() throws ThingException, ExecutionException, InterruptedException {

        final Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]);
        final ThingFuture<Void> future = tPuppet.getThingCom(NlpThingCom.class, true)
                .getRecorder(mixer)
                .recording(new RecordHandler() {
                    @Override
                    public void onSentence(String recordId, int index, String text, double confidence) {
                        System.out.printf("recordId=%s;index=%s;confidence=%s;text=%s%n",
                                recordId, index, confidence, text
                        );
                    }
                })
                .syncUninterruptible();

    }

    @Test
    public void recorder$record() throws ThingException, ExecutionException, InterruptedException {

        final Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]);
        final ThingFuture<Void> future = tPuppet.getThingCom(NlpThingCom.class, true)
                .getRecorder(mixer)
                .record(new RecordHandler() {
                    @Override
                    public void onSentence(String recordId, int index, String text, double confidence) {
                        System.out.printf("recordId=%s;index=%s;confidence=%s;text=%s%n",
                                recordId, index, confidence, text
                        );
                    }
                })
                .syncUninterruptible();
    }

    @Test
    public void recordingForWakeUp() throws ThingException, ExecutionException {
        final Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]);
        final ThingFuture<Void> future = tPuppet.getThingCom(NlpThingCom.class, true)
                .getRecorder(mixer)
                .recordingForWakeUp(() -> {
                    System.out.println("WAKEUP!!");
                })
                .syncUninterruptible();
    }

    /**
     * 录制关键词
     */
    @Test
    public void recordingForHotWords() throws LineUnavailableException, IOException {

        final File file = new File("/home/vlinux/xiaokun.wav");
        final AudioFormat format = new AudioFormat(
                16000f,
                16,
                1,
                true,
                false
        );

        System.out.println("BEGIN!");
        final TargetDataLine target = AudioSystem.getTargetDataLine(format, AudioSystem.getMixerInfo()[0]);
        target.open(format);
        target.start();

        try (final ByteOutputStream bos = new ByteOutputStream()) {
            final byte[] buffer = new byte[format.getFrameSize() * 1024];
            final long begin = System.currentTimeMillis();
            do {
                final int size = target.read(buffer, 0, buffer.length);
                bos.write(buffer, 0, size);
            } while (System.currentTimeMillis() - begin < 2000);
            try (final AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(bos.getBytes()), format, bos.getCount())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
            }
        }

        target.stop();

    }

}
