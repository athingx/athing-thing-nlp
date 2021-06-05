package com.github.athingx.athing.aliyun.nlp.boot;

import com.github.athingx.athing.aliyun.nlp.component.util.SystemUtils;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.junit.Test;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class TestCase {

    @Test
    public void test2() throws LineUnavailableException, IOException, InterruptedException {

        final AudioFormat format = new AudioFormat(
                48000f,
                16,
                2,
                true,
                false
        );

        final SourceDataLine source = AudioSystem.getSourceDataLine(format, AudioSystem.getMixerInfo()[0]);

        source.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                System.out.println(event);
            }
        });

        source.open(format);
        source.start();

        final byte[] data = new byte[1024];


        try (final FileInputStream fis = new FileInputStream("/home/vlinux/229189149233159179.wav")) {

            int len = 0;

            while ((len = fis.read(data)) != -1) {
                source.write(data, 0, len);
            }

        } finally {
            source.stop();
            source.close();
        }

    }

    @Test
    public void test3() throws IOException, LineUnavailableException {

        final AudioFormat format = new AudioFormat(
                16000f,
                16,
                2,
                true,
                false
        );

        final TargetDataLine target = AudioSystem.getTargetDataLine(format, AudioSystem.getMixerInfo()[0]);
        target.open(format);

        final SourceDataLine source = AudioSystem.getSourceDataLine(format, AudioSystem.getMixerInfo()[0]);
        source.open(format);

        target.start();
        source.start();

        final byte[] data = new byte[10240];
        while (true) {

            final int len = target.read(data, 0, data.length);
            source.write(data, 0, len);

        }


    }

    @Test
    public void test5() throws LineUnavailableException, IOException {

        final AudioFormat format = new AudioFormat(
                16000f,
                16,
                2,
                true,
                false
        );

        final TargetDataLine target = AudioSystem.getTargetDataLine(format, AudioSystem.getMixerInfo()[0]);
        System.out.println("BEGIN!");
        target.open(format);
        target.start();

        final ByteOutputStream bos = new ByteOutputStream();
        final byte[] buffer = new byte[format.getFrameSize() * 1024];
        final long begin = System.currentTimeMillis();
        while (true) {
            final int size = target.read(buffer, 0, buffer.length);
            bos.write(buffer, 0, size);
            if (System.currentTimeMillis() - begin >= 2000) {
                break;
            }
        }

        final AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(bos.getBytes()), format, bos.getCount());
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File("/home/vlinux/xiaokun003.wav"));

    }

    @Test
    public void test6() {
        System.out.println(SystemUtils.getArchBit());
    }

}
