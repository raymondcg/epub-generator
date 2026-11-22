package org.webnerdbot.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

/**
 * Loads the repo-root {@code .env} into system properties for Spring {@code @Value} binding. Does not override keys
 * already set as system properties or environment variables.
 */
public final class EnvLoader {

    private static final Logger LOGGER = LogManager.getLogger( EnvLoader.class );
    private static final String ENV_FILE = ".env";

    private EnvLoader() {
    }

    public static void load() {
        Path envDir = findEnvDirectory();
        if ( envDir == null ) {
            LOGGER.warn( "No {} found walking up from {}; relying on existing environment/system properties.",
                    ENV_FILE, Paths.get( "" ).toAbsolutePath() );
            return;
        }

        Dotenv dotenv = Dotenv.configure().directory( envDir.toString() ).filename( ENV_FILE ).ignoreIfMalformed()
                .load();

        for ( DotenvEntry entry : dotenv.entries() ) {
            String key = entry.getKey();
            if ( System.getProperty( key ) == null && System.getenv( key ) == null ) {
                System.setProperty( key, entry.getValue() );
            }
        }
        LOGGER.info( "Loaded environment from {}", envDir.resolve( ENV_FILE ) );
    }

    private static Path findEnvDirectory() {
        Path dir = Paths.get( "" ).toAbsolutePath().normalize();
        for ( int i = 0; i < 6 && dir != null; i++ ) {
            if ( Files.isRegularFile( dir.resolve( ENV_FILE ) ) ) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
