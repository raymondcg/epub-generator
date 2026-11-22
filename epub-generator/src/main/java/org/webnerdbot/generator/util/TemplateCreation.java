package org.webnerdbot.generator.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateCreation {

    private static final Logger LOGGER = LogManager.getLogger( TemplateCreation.class );

    private Configuration cfg;

    public TemplateCreation( Configuration cfg ) {
        this.cfg = cfg;
    }

    public String createString( Map<String, Object> input, String templateName ) {
        try (StringWriter stringWriter = new StringWriter()) {
            Template template = cfg.getTemplate( templateName );
            template.process( input, stringWriter );
            return stringWriter.toString();
        } catch ( IOException e ) {
            LOGGER.error( "Error Processing Template {}", templateName, e );
        } catch ( TemplateException e ) {
            LOGGER.error( "Error Processing Template {}", templateName, e );
        }
        return "";
    }
}
