package com.github.athingx.athing.aliyun.nlp.api;

import com.github.athingx.athing.standard.component.ThingCom;

import javax.sound.sampled.Mixer;

public interface NlpThingCom extends ThingCom {

    /**
     * 获取演讲者
     *
     * @param mixer 混音器
     *              指定演讲输出的设备
     * @return 演讲者
     */
    Speaker getSpeaker(Mixer mixer);

    /**
     * 获取速记员
     *
     * @param mixer 混音器
     *              指定速记员输入的设备
     * @return 速记员
     */
    Recorder getRecorder(Mixer mixer);

}
