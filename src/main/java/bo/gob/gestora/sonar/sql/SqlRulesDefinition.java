package bo.gob.gestora.sonar.sql;

import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class SqlRulesDefinition implements RulesDefinition {
    public static final String REPOSITORY_KEY = "gestora-sql-rules";

    @Override
    public void define(Context context) {
        NewRepository repo = context.createRepository(REPOSITORY_KEY, SqlLanguage.KEY)
                .setName("Reglas de Calidad SQL Gestora");

        repo.createRule("SQL_001")
                .setName("Proyección Indiscriminada (SELECT *)")
                .setHtmlDescription("<p>Evite usar SELECT * para no sobrecargar la memoria ni impedir el uso de índices cubrientes.</p>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL);

        repo.createRule("SQL_002")
                .setName("Función Escalar en Predicado (Non-Sargable)")
                .setHtmlDescription("<p>No aplique funciones escalares directamente sobre columnas en la cláusula WHERE, ya que anula el uso de índices.</p>")
                .setSeverity(Severity.CRITICAL)
                .setType(RuleType.CODE_SMELL);

        repo.createRule("SQL_003")
                .setName("Mutación DML sin Filtro WHERE")
                .setHtmlDescription("<p>Toda sentencia UPDATE o DELETE debe incluir una cláusula WHERE para prevenir alteraciones o borrados masivos.</p>")
                .setSeverity(Severity.BLOCKER)
                .setType(RuleType.BUG);

        repo.createRule("SQL_004")
                .setName("Vulnerabilidad de Inyección SQL (SQLi)")
                .setHtmlDescription("<p>Se detectó concatenación dinámica no parametrizada en rutinas de ejecución.</p>")
                .setSeverity(Severity.BLOCKER)
                .setType(RuleType.VULNERABILITY);

        repo.createRule("SQL_005")
                .setName("Ausencia de Control Transaccional")
                .setHtmlDescription("<p>Los scripts con múltiples operaciones de escritura DML deben usar BEGIN TRANSACTION y COMMIT/ROLLBACK.</p>")
                .setSeverity(Severity.MAJOR)
                .setType(RuleType.CODE_SMELL);

        repo.done();
    }
}