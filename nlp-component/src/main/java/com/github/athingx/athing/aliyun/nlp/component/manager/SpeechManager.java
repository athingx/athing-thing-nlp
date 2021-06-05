package com.github.athingx.athing.aliyun.nlp.component.manager;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.github.athingx.athing.aliyun.nlp.api.Speaker;
import com.github.athingx.athing.aliyun.nlp.component.NlpOption;
import com.github.athingx.athing.aliyun.thing.runtime.executor.ThingExecutor;
import com.github.athingx.athing.standard.thing.ThingFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;

public class SpeechManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final NlsClient client;
    private final ThingExecutor executor;
    private final NlpOption option;
    private final AudioFormat format;
    private final String _string;

    public SpeechManager(NlsClient client, ThingExecutor executor, NlpOption option) {
        this.client = client;
        this.executor = executor;
        this.option = option;
        this.format = getFormat(option);
        this._string = String.format("nlp:/%s/speech", option.getAppKey());
    }

    @Override
    public String toString() {
        return _string;
    }

    /**
     * 获取音频格式
     *
     * @param option 启动项
     * @return 音频格式
     */
    private AudioFormat getFormat(NlpOption option) {
        return new AudioFormat(
                option.getSampleRate(),
                16,
                1,
                true,
                false
        );
    }

    public Speaker getSpeaker(Mixer mixer) {
        return new Speaker() {

            private final String _string = String.format("%s/speaker[\"%s\":%s]",
                    SpeechManager.this._string,
                    mixer.getMixerInfo().getName(),
                    mixer.getMixerInfo().getVersion()
            );

            @Override
            public String toString() {
                return _string;
            }

            /**
             * 初始化音频线路
             *
             * @param mixer 混音器
             * @return 音频线路
             * @throws LineUnavailableException 线路不支持
             */
            private SourceDataLine openSourceDataLine(Mixer mixer, AudioFormat format) throws LineUnavailableException {
                final SourceDataLine source = AudioSystem.getSourceDataLine(format, mixer.getMixerInfo());
                source.addLineListener(event -> logger.debug("{} source: {}, line: {}", this, event.getType(), event.getLine().getLineInfo()));
                source.open(format);
                return source;
            }

            @Override
            public ThingFuture<Void> speak(String text) {
                return executor.promise(promise -> {

                    // 打开音频线路
                    final SourceDataLine source = openSourceDataLine(mixer, format);
                    promise.onDone(future -> {
                        source.stop();
                        source.close();
                    });

                    // 构建演讲同步器
                    final SpeechSynthesizer synthesizer = new SpeechSynthesizer(client, new SpeechSynthesizerListener() {

                        @Override
                        public void onComplete(SpeechSynthesizerResponse response) {
                            source.drain();
                            promise.trySuccess();
                        }

                        @Override
                        public void onFail(SpeechSynthesizerResponse response) {
                            promise.tryException(new IllegalStateException(String.format("illegal state: %s, taskId=%s;reason=%s;",
                                    response.getStatus(),
                                    response.getTaskId(),
                                    response.getStatusText()
                            )));
                        }

                        @Override
                        public void onMessage(ByteBuffer buffer) {
                            try {

                                final byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);
                                int offset = 0;
                                do {
                                    final int count = source.write(data, offset, data.length - offset);
                                    if (count == 0) {
                                        break;
                                    }
                                    offset += count;
                                } while (offset < data.length);

                            } catch (Exception cause) {
                                promise.tryException(cause);
                            }
                        }

                    });
                    promise.onDone(future -> synthesizer.close());
                    synthesizer.setAppKey(option.getAppKey());
                    synthesizer.setFormat(OutputFormatEnum.PCM);
                    synthesizer.setSampleRate((int) option.getSampleRate());
                    synthesizer.setText(text);

                    // 开始演讲
                    source.start();
                    synthesizer.start();

                });

            }

        };
    }

}
