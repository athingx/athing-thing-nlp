package com.github.athingx.athing.aliyun.nlp.boot;

import com.github.athingx.athing.aliyun.nlp.component.NlpOption;
import com.github.athingx.athing.aliyun.nlp.component.NlpThingComImpl;
import com.github.athingx.athing.standard.component.ThingCom;
import com.github.athingx.athing.standard.thing.boot.BootArguments;
import com.github.athingx.athing.standard.thing.boot.ThingComBoot;
import org.kohsuke.MetaInfServices;

import java.util.Objects;

import static com.github.athingx.athing.standard.thing.boot.BootArguments.Converter.cFloat;
import static com.github.athingx.athing.standard.thing.boot.BootArguments.Converter.cString;

@MetaInfServices
public class BootImpl implements ThingComBoot {

    public static final String ACCESS_KEY_ID = "access-key-id";
    public static final String ACCESS_KEY_SECRET = "access-key-secret";
    public static final String REMOTE = "remote";
    public static final String APP_KEY = "app-key";
    public static final String SAMPLE_RATE = "sample-rate";

    @Override
    public ThingCom bootUp(String productId, String thingId, BootArguments arguments) {

        final NlpOption option = new NlpOption();
        option.setAccessKeyId(Objects.requireNonNull(arguments.getArgument(ACCESS_KEY_ID, cString)));
        option.setAccessKeySecret(Objects.requireNonNull(arguments.getArgument(ACCESS_KEY_SECRET, cString)));
        option.setAppKey(Objects.requireNonNull(arguments.getArgument(APP_KEY, cString)));
        option.setRemote(arguments.getArgument(REMOTE, cString, "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1"));
        option.setSampleRate(arguments.getArgument(SAMPLE_RATE, cFloat, 8000f));

        return new NlpThingComImpl(option);
    }

}
