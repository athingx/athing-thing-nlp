package com.github.athingx.athing.aliyun.nlp.component.manager;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.*;
import com.github.athingx.athing.aliyun.nlp.api.RecordHandler;
import com.github.athingx.athing.aliyun.nlp.api.Recorder;
import com.github.athingx.athing.aliyun.nlp.api.WakeUpHandler;
import com.github.athingx.athing.aliyun.nlp.component.NlpOption;
import com.github.athingx.athing.aliyun.nlp.component.snowboy.Snowboy;
import com.github.athingx.athing.aliyun.thing.runtime.executor.ThingExecutor;
import com.github.athingx.athing.standard.thing.ThingFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;

public class RecordManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final NlsClient client;
    private final ThingExecutor executor;
    private final NlpOption option;
    private final AudioFormat format;
    private final String _string;

    public RecordManager(NlsClient client, ThingExecutor executor, NlpOption option) {
        this.client = client;
        this.executor = executor;
        this.option = option;
        this.format = getFormat(option);
        this._string = String.format("nlp:/%s/record", option.getAppKey());
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


    /**
     * 初始化输入线路
     *
     * @param tag    标签
     *               用于区别recording和record
     * @param mixer  混音器
     * @param format 音频格式
     * @return 音频输入线路
     * @throws LineUnavailableException 线路不可用
     */
    private TargetDataLine openTargetDataLine(String tag, Mixer mixer, AudioFormat format) throws LineUnavailableException {
        final TargetDataLine target = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo());
        target.addLineListener(event -> logger.debug("{}/{} target: {}, line: {}", this, tag, event.getType(), event.getLine().getLineInfo()));
        target.open(format);
        return target;
    }

    /**
     * 获取采样率
     *
     * @return 采样率
     */
    private SampleRateEnum getSampleRate() {
        final float sampleRate = option.getSampleRate();
        if (sampleRate == 16000f) {
            return SampleRateEnum.SAMPLE_RATE_16K;
        }
        if (sampleRate == 8000f) {
            return SampleRateEnum.SAMPLE_RATE_8K;
        }
        throw new IllegalArgumentException("illegal sample-rate: " + sampleRate);
    }


    /**
     * 获取速记员
     *
     * @param mixer 混音器
     *              指定速记员输入的设备
     * @return 速记员
     */
    public Recorder getRecorder(Mixer mixer) {
        return new Recorder() {

            @Override
            public ThingFuture<Void> recordingForWakeUp(WakeUpHandler handler) {
                return executor.promise(promise -> {

                    final Snowboy snowboy = new Snowboy();
                    promise.onDone(future -> snowboy.close());

                    // 构建音频输入
                    final TargetDataLine target = openTargetDataLine("recording-for-wakeup", mixer, format);
                    promise.onDone(future -> {
                        target.stop();
                        target.close();
                    });

                    target.start();

                    final byte[] data = new byte[format.getFrameSize() * 1024];
                    while (!promise.isDone()) {

                        final int size = target.read(data, 0, data.length);
                        if (snowboy.detect(data, 0, size)) {
                            handler.onWakeUp();
                        }

                    }

                });
            }

            @Override
            public ThingFuture<Void> recording(RecordHandler handler) {
                return executor.promise(promise -> {

                    // 构建音频输入
                    final TargetDataLine target = openTargetDataLine("recording", mixer, format);
                    promise.onDone(future -> {
                        target.stop();
                        target.close();
                    });

                    // 构建同步器
                    final SpeechTranscriber transcriber = new SpeechTranscriber(client, new SpeechTranscriberListener() {

                        @Override
                        public void onTranscriberStart(SpeechTranscriberResponse response) {

                        }

                        @Override
                        public void onSentenceBegin(SpeechTranscriberResponse response) {

                        }

                        @Override
                        public void onSentenceEnd(SpeechTranscriberResponse response) {
                            handler.onSentence(
                                    response.getTaskId(),
                                    response.getTransSentenceIndex(),
                                    response.getTransSentenceText(),
                                    response.getConfidence()
                            );
                        }

                        @Override
                        public void onTranscriptionResultChange(SpeechTranscriberResponse response) {

                        }

                        @Override
                        public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                            promise.trySuccess();
                        }

                        @Override
                        public void onFail(SpeechTranscriberResponse response) {
                            promise.tryException(new IllegalStateException(String.format("illegal state: %s, taskId=%s;reason=%s;",
                                    response.getStatus(),
                                    response.getTaskId(),
                                    response.getStatusText()
                            )));
                        }

                    });
                    promise.onDone(future -> {
                        transcriber.stop();
                        transcriber.close();
                    });
                    transcriber.setAppKey(option.getAppKey());
                    transcriber.setFormat(InputFormatEnum.PCM);
                    transcriber.setSampleRate(getSampleRate());

                    // 开始速记
                    target.start();
                    transcriber.start();

                    // 循环开始
                    final byte[] data = new byte[format.getFrameSize() * 1024];
                    while (!promise.isDone()) {
                        final int size = target.read(data, 0, data.length);
                        transcriber.send(data, size);
                    }

                });
            }

            @Override
            public ThingFuture<Void> record(RecordHandler handler) {
                return executor.promise(promise -> {

                    // 构建音频输入
                    final TargetDataLine target = openTargetDataLine("record", mixer, format);
                    promise.onDone(future -> {
                        target.stop();
                        target.close();
                    });

                    // 构建同步器
                    final SpeechRecognizer recognizer = new SpeechRecognizer(client, new SpeechRecognizerListener() {
                        @Override
                        public void onRecognitionResultChanged(SpeechRecognizerResponse response) {

                        }

                        @Override
                        public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                            handler.onSentence(
                                    response.getTaskId(),
                                    1,
                                    response.getRecognizedText(),
                                    0d
                            );
                            promise.trySuccess();
                        }

                        @Override
                        public void onStarted(SpeechRecognizerResponse response) {

                        }

                        @Override
                        public void onFail(SpeechRecognizerResponse response) {
                            promise.tryException(new IllegalStateException(String.format("illegal state: %s, taskId=%s;reason=%s;",
                                    response.getStatus(),
                                    response.getTaskId(),
                                    response.getStatusText()
                            )));
                        }
                    });
                    promise.onDone(future -> {
                        recognizer.stop();
                        recognizer.close();
                    });
                    recognizer.setAppKey(option.getAppKey());
                    recognizer.setFormat(InputFormatEnum.PCM);
                    recognizer.setSampleRate(getSampleRate());
                    recognizer.setEnableITN(true);
                    recognizer.payload.put("enable_voice_detection", true);
                    recognizer.payload.put("max_start_silence", 10000);
                    recognizer.payload.put("max_end_silence", 1000);

                    // 开始识别
                    target.start();
                    recognizer.start();

                    // 循环开始
                    final byte[] data = new byte[1024];
                    while (!promise.isDone()) {
                        final int size = target.read(data, 0, data.length);
                        recognizer.send(data, size);
                    }

                });
            }

        };
    }

}
