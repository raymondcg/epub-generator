package org.webnerdbot.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.webnerdbot.dao.DataAccess;
import org.webnerdbot.dao.file.BookDao;
import org.webnerdbot.dao.file.DataAccessImpl;
import org.webnerdbot.generator.GeneratorService;
import org.webnerdbot.generator.util.FileHelper;
import org.webnerdbot.generator.util.TemplateCreation;

import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

@Configuration
public class CommonConfig {

    @Bean
    public GeneratorService generatorService( ApplicationProperties applicationProperties,
            TemplateCreation templateCreation, FileHelper fileHelper, BookDao bookDao ) {
        return new GeneratorService( applicationProperties, templateCreation, fileHelper, bookDao );
    }

    @Bean
    public FileHelper fileHelper( ApplicationProperties applicationProperties ) {
        return new FileHelper( applicationProperties );
    }

    @Bean
    public DataAccess dataAccess() {
        return new DataAccessImpl();
    }

    @Bean
    public TemplateCreation templateCreation() {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration( new Version( "2.3.23" ) );
        cfg.setClassForTemplateLoading( TemplateCreation.class, "/templates" );
        cfg.setDefaultEncoding( "UTF-8" );
        cfg.setLocale( Locale.US );
        cfg.setTemplateExceptionHandler( TemplateExceptionHandler.RETHROW_HANDLER );
        return new TemplateCreation( cfg );
    }

    @Bean
    public BookDao bookDao( DataAccess dataAccess, ApplicationProperties applicationProperties ) {
        return new BookDao( dataAccess, applicationProperties );
    }
}
