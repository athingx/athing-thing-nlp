package com.github.athingx.athing.aliyun.nlp.component;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import com.github.athingx.athing.aliyun.nlp.api.NlpThingCom;
import com.github.athingx.athing.aliyun.nlp.api.Recorder;
import com.github.athingx.athing.aliyun.nlp.api.Speaker;
import com.github.athingx.athing.aliyun.nlp.component.manager.RecordManager;
import com.github.athingx.athing.aliyun.nlp.component.manager.SpeechManager;
import com.github.athingx.athing.aliyun.thing.runtime.ThingRuntime;
import com.github.athingx.athing.aliyun.thing.runtime.ThingRuntimes;
import com.github.athingx.athing.aliyun.thing.runtime.executor.ThingExecutor;
import com.github.athingx.athing.standard.thing.Thing;
import com.github.athingx.athing.standard.thing.ThingFuture;
import com.github.athingx.athing.standard.thing.boot.Disposable;
import com.github.athingx.athing.standard.thing.boot.Initializing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class NlpThingComImpl implements NlpThingCom, Initializing, Disposable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NlsClient client;
    private final NlpOption option;
    private final String _string;

    private ThingExecutor executor;
    private SpeechManager speechManager;
    private RecordManager recordManager;

    private volatile boolean isDestroy = false;
    private volatile ThingFuture<Void> flushF;

    public NlpThingComImpl(NlpOption option) {
        this.option = option;
        this.client = new NlsClient(option.getRemote());
        this._string = String.format("nlp:/%s", option.getAppKey());
    }

    @Override
    public String toString() {
        return _string;
    }

    @Override
    public Speaker getSpeaker(Mixer mixer) {
        return speechManager.getSpeaker(mixer);
    }

    @Override
    public Recorder getRecorder(Mixer mixer) {
        return recordManager.getRecorder(mixer);
    }

    @Override
    public void onDestroyed() {

        isDestroy = true;

        if (null != flushF) {
            flushF.cancel(true);
        }

        client.shutdown();

    }

    /**
     * 定时刷新TOKEN
     */
    private void flush() {

        // AccessToken刷新
        final AccessToken token = new AccessToken(option.getAccessKeyId(), option.getAccessKeySecret());

        // 如果失败，每隔10s尝试刷新token
        final long delay = TimeUnit.SECONDS.toMillis(10);

        final Runnable flusher = new Runnable() {
            @Override
            public void run() {

                if (isDestroy) {
                    return;
                }

                // 刷新TOKEN
                try {
                    token.apply();
                    client.setToken(token.getToken());
                    final long next = System.currentTimeMillis() + token.getExpireTime();
                    logger.info("{} flush token success, expire=\"{}\"",
                            NlpThingComImpl.this,
                            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(next))
                    );
                    flushF = executor.submit(next, MILLISECONDS, this);
                }

                // 刷新失败
                catch (Throwable cause) {
                    logger.warn("{} flush token failure, will retry after {} ms", NlpThingComImpl.this, delay, cause);
                    flushF = executor.submit(delay, MILLISECONDS, this);
                }

            }
        };

        // 开始刷新
        flusher.run();

    }

    @Override
    public void onInitialized(Thing thing) {

        final ThingRuntime runtime = ThingRuntimes.getThingRuntime(thing);
        this.executor = runtime.getThingExecutor();
        this.speechManager = new SpeechManager(client, executor, option);
        this.recordManager = new RecordManager(client, executor, option);

        flush();

    }

}
