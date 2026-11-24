package bo.gob.gestora.sonar.sql;

import org.sonar.api.Plugin;

public class SqlCustomPlugin implements Plugin {
    @Override
    public void define(Context context) {
        context.addExtensions(
                SqlLanguage.class,
                SqlRulesDefinition.class,
                SqlQualityProfile.class,
                SqlSensor.class
        );
    }
}