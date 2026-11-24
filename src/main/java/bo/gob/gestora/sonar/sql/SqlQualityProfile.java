package bo.gob.gestora.sonar.sql;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public class SqlQualityProfile implements BuiltInQualityProfilesDefinition {
    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile("Gestora SQL Way", SqlLanguage.KEY);
        profile.setDefault(true);

        profile.activateRule(SqlRulesDefinition.REPOSITORY_KEY, "SQL_001");
        profile.activateRule(SqlRulesDefinition.REPOSITORY_KEY, "SQL_002");
        profile.activateRule(SqlRulesDefinition.REPOSITORY_KEY, "SQL_003");
        profile.activateRule(SqlRulesDefinition.REPOSITORY_KEY, "SQL_004");
        profile.activateRule(SqlRulesDefinition.REPOSITORY_KEY, "SQL_005");

        profile.done();
    }
}