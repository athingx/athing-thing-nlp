package com.github.athingx.athing.aliyun.nlp.api;

import com.github.athingx.athing.standard.thing.ThingFuture;

/**
 * 速记员
 */
public interface Recorder {

    /**
     * 持续唤醒
     * @param handler 唤醒处理器
     * @return 唤醒凭证
     */
    ThingFuture<Void> recordingForWakeUp(WakeUpHandler handler);

    /**
     * 持续记录
     * <p>
     * 持续记录开始后将会不停的记录下所听到的句子，并交给速记处理器处理。
     * 直到{@link ThingFuture#cancel(boolean)}被调用为止
     * </p>
     *
     * @param handler 速记处理器
     * @return 速记凭证
     */
    ThingFuture<Void> recording(RecordHandler handler);

    /**
     * 短句记录
     * <p>
     * 短句记录开始后将会记录下所听到的第一个完整句子，并交给速记处理器处理。
     * 直到第一个短句说完或直到{@link ThingFuture#cancel(boolean)}被调用为止
     * </p>
     *
     * @param handler 速记处理器
     * @return 速记凭证
     */
    ThingFuture<Void> record(RecordHandler handler);

}
