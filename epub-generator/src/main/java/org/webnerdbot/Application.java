package org.webnerdbot;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.webnerdbot.config.EnvLoader;
import org.webnerdbot.generator.GeneratorService;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private Environment environment;

    public static void main( String[] args ) {
        EnvLoader.load();
        SpringApplication.run( Application.class, args );
    }

    @Override
    public void run( String... args ) {
        if ( Arrays.asList( environment.getActiveProfiles() ).contains( "test" ) ) {
            return;
        }
        generatorService.go();
    }
}
