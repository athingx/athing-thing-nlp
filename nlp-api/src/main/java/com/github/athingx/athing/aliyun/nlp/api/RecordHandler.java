package com.github.athingx.athing.aliyun.nlp.api;

/**
 * 速记处理器
 */
public interface RecordHandler {

    /**
     * 记录句子
     *
     * @param recordId   记录ID
     * @param index      句子编号
     * @param text       句子文本
     * @param confidence 识别置信度
     */
    void onSentence(String recordId, int index, String text, double confidence);

}
