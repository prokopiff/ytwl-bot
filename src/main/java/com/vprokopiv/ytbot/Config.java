package com.vprokopiv.ytbot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class);

    private static final Properties PROPS;

    static {
        PROPS = new Properties();
        var propsPath = Optional.ofNullable(System.getProperty("application.properties"));
        propsPath.ifPresentOrElse(
                path -> {
                    try (FileReader fr = new FileReader(path)) {
                        PROPS.load(fr);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                },
                () -> {
                    try {
                        PROPS.load(ClassLoader.getSystemResourceAsStream("application.properties"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private Config() {}

    public static Optional<String> getProperty(String key) {
        var sys = Optional.ofNullable(System.getProperty(key));
        return sys.or(() -> Optional.ofNullable(PROPS.getProperty(key)));
    }

    public static String getRequiredProperty(String key) {
        return getProperty(key).orElseThrow();
    }
}
