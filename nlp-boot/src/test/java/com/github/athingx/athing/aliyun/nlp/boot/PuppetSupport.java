package com.github.athingx.athing.aliyun.nlp.boot;

import com.github.athingx.athing.aliyun.thing.ThingBoot;
import com.github.athingx.athing.aliyun.thing.runtime.access.ThingAccess;
import com.github.athingx.athing.aliyun.thing.runtime.access.ThingAccessImpl;
import com.github.athingx.athing.standard.thing.Thing;
import com.github.athingx.athing.standard.thing.boot.BootArguments;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public class PuppetSupport {

    protected static final Properties properties = getProperties(new Properties());
    protected static final String PRODUCT_ID =
            properties.getProperty("athing.product.id");
    protected static final String THING_ID =
            properties.getProperty("athing.thing.id");
    protected static final String THING_SERVER_URL =
            properties.getProperty("athing.thing.server-url");
    protected static final ThingAccess THING_ACCESS = new ThingAccessImpl(
            PRODUCT_ID,
            THING_ID,
            properties.getProperty("athing.thing.secret")
    );

    protected static Thing tPuppet;

    /**
     * 初始化配置文件
     *
     * @param properties 配置信息
     * @return 配置信息
     */
    private static Properties getProperties(Properties properties) {
        // 读取配置文件
        final String propertiesFilePath = System.getProperties().getProperty("athing-qatest.properties.file");
        final File propertiesFile = new File(propertiesFilePath);
        if (!propertiesFile.exists() || !propertiesFile.canRead()) {
            throw new RuntimeException(String.format("loading properties error: file not existed: %s", propertiesFilePath));
        }
        try (final InputStream is = new FileInputStream(propertiesFile)) {
            properties.load(is);
            return properties;
        } catch (Exception cause) {
            throw new RuntimeException("loading properties error!", cause);
        }
    }


    @BeforeClass
    public static void initialization() throws Exception {
        tPuppet = initPuppetThing();
    }

    @AfterClass
    public static void destroy() throws Exception {
        if (null != tPuppet) {
            tPuppet.destroy();
        }
    }

    private static Thing initPuppetThing() throws Exception {
        final Thing thing = new ThingBoot(new URI(THING_SERVER_URL), THING_ACCESS)
                .load(new BootImpl().bootUp(PRODUCT_ID, THING_ID, BootArguments.parse(
                        String.format(
                                "access-key-id=%s&access-key-secret=%s&app-key=%s",
                                properties.getProperty("athing-platform.access.id"),
                                properties.getProperty("athing-platform.access.secret"),
                                "5mjOzsPzKtTN1jfs"
                        )
                )))
                .boot();

        reconnect(thing);

        return thing;
    }

    private static void reconnect(Thing thing) {
        if (thing.isDestroyed()) {
            return;
        }

        thing.getThingOp().connect()
                .onFailure(connF -> {
                    reconnect(thing);
                })
                .onSuccess(connF -> {
                    connF.getSuccess().getDisconnectFuture().onDone(disconnectF -> {
                        reconnect(thing);
                    });
                });

    }

}
