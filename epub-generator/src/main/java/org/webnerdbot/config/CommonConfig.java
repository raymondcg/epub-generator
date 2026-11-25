package org.webnerdbot.config;

import java.util.Locale;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.webnerdbot.generator.CustomReportStore;
import org.webnerdbot.generator.GeneratorService;
import org.webnerdbot.generator.util.FileHelper;
import org.webnerdbot.generator.util.TemplateCreation;

import freemarker.template.TemplateExceptionHandler;

@Configuration
@EnableConfigurationProperties(ReportStoreProperties.class)
public class CommonConfig {

    @Bean
    public GeneratorService generatorService( TemplateCreation templateCreation, FileHelper fileHelper,
            CustomReportStore customReportStore ) {
        return new GeneratorService( templateCreation, fileHelper, customReportStore );
    }

    @Bean
    public FileHelper fileHelper() {
        return new FileHelper();
    }

    @Bean
    public CustomReportStore customReportStore( ReportStoreProperties reportStoreProperties ) {
        return new CustomReportStore( reportStoreProperties.getMaxEntries() );
    }

    @Bean
    public TemplateCreation templateCreation() {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(
                freemarker.template.Configuration.VERSION_2_3_35 );
        cfg.setClassForTemplateLoading( TemplateCreation.class, "/templates" );
        cfg.setDefaultEncoding( "UTF-8" );
        cfg.setLocale( Locale.US );
        cfg.setTemplateExceptionHandler( TemplateExceptionHandler.RETHROW_HANDLER );
        return new TemplateCreation( cfg );
    }
}
