package org.webnerdbot.config;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;

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
    private static final int MAX_PARENT_WALK = 6;

    private EnvLoader() {
    }

    public static void load() {
        Path envDir = findEnvDirectory();
        if ( envDir == null ) {
            LOGGER.warn( "No {} found walking up from {}; relying on existing environment/system properties.", ENV_FILE,
                    Paths.get( "" ).toAbsolutePath() );
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
        Path fromCwd = walkForEnv( Paths.get( "" ).toAbsolutePath().normalize() );
        if ( fromCwd != null ) {
            return fromCwd;
        }
        return walkForEnv( codeSourceDirectory() );
    }

    private static Path walkForEnv( Path start ) {
        if ( start == null ) {
            return null;
        }
        Path dir = start.normalize();
        for ( int i = 0; i < MAX_PARENT_WALK && dir != null; i++ ) {
            if ( Files.isRegularFile( dir.resolve( ENV_FILE ) ) ) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private static Path codeSourceDirectory() {
        try {
            ProtectionDomain domain = EnvLoader.class.getProtectionDomain();
            if ( domain == null ) {
                return null;
            }
            CodeSource codeSource = domain.getCodeSource();
            if ( codeSource == null ) {
                return null;
            }
            URL location = codeSource.getLocation();
            if ( location == null ) {
                return null;
            }
            Path path = Paths.get( location.toURI() );
            return Files.isRegularFile( path ) ? path.getParent() : path;
        } catch ( URISyntaxException | IllegalArgumentException e ) {
            LOGGER.debug( "Could not resolve code source directory for {} lookup.", ENV_FILE, e );
            return null;
        }
    }
}
