package org.webnerdbot.generator.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateCreation {

    private final Configuration cfg;

    public TemplateCreation( Configuration cfg ) {
        this.cfg = cfg;
    }

    public String createString( Map<String, Object> input, String templateName ) {
        try (StringWriter stringWriter = new StringWriter()) {
            Template template = cfg.getTemplate( templateName );
            template.process( input, stringWriter );
            return stringWriter.toString();
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Error processing template " + templateName, e );
        } catch ( TemplateException e ) {
            throw new IllegalStateException( "Error processing template " + templateName, e );
        }
    }
}
