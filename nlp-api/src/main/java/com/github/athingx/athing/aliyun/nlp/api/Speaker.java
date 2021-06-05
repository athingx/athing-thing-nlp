package com.github.athingx.athing.aliyun.nlp.api;

import com.github.athingx.athing.standard.thing.ThingFuture;

/**
 * 演讲者
 */
public interface Speaker {

    /**
     * 发起演讲
     *
     * @param text    演讲内容
     * @return 演讲凭证
     */
    ThingFuture<Void> speak(String text);

}
