package bo.gob.gestora.sonar.sql;

import org.sonar.api.resources.AbstractLanguage;

public class SqlLanguage extends AbstractLanguage {
    public static final String KEY = "sql";

    public SqlLanguage() {
        super(KEY, "SQL");
    }

    @Override
    public String[] getFileSuffixes() {
        return new String[]{".sql"};
    }
}