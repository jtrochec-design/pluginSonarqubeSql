package bo.gob.gestora.sonar.sql;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class SqlSensor implements Sensor {
    private static final Logger LOG = Loggers.get(SqlSensor.class);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Analizador Sintáctico AST de Consultas SQL")
                .onlyOnLanguage(SqlLanguage.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        Iterable<InputFile> files = context.fileSystem().inputFiles(
                context.fileSystem().predicates().hasLanguage(SqlLanguage.KEY));

        for (InputFile file : files) {
            try {
                String content = file.contents();
                Statements statements = CCJSqlParserUtil.parseStatements(content);
                SqlAstVisitor visitor = new SqlAstVisitor(context, file);
                
                int ncloc = 0;
                for (String line : content.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        ncloc++;
                    }
                }
                context.<Integer>newMeasure()
                        .forMetric(CoreMetrics.NCLOC)
                        .on(file)
                        .withValue(ncloc)
                        .save();

                for (Statement stmt : statements.getStatements()) {
                    stmt.accept(visitor);
                }

                // SQL_005: Control Transaccional si existen múltiples operaciones DML
                String upperContent = content.toUpperCase();
                if (visitor.getDmlCount() > 1) {
                    boolean hasBeginTran = upperContent.contains("BEGIN TRAN");
                    boolean hasCommitOrRollback = upperContent.contains("COMMIT") || upperContent.contains("ROLLBACK");
                    if (!hasBeginTran || !hasCommitOrRollback) {
                        NewIssue newIssue = context.newIssue()
                                .forRule(RuleKey.of(SqlRulesDefinition.REPOSITORY_KEY, "SQL_005"));
                        NewIssueLocation location = newIssue.newLocation()
                                .on(file)
                                .at(file.selectLine(1))
                                .message("Estándar: Múltiples operaciones DML sin bloque explícito BEGIN TRANSACTION y COMMIT/ROLLBACK.");
                        newIssue.at(location).save();
                    }
                }
            } catch (Exception e) {
                LOG.warn("No se pudo parsear el archivo SQL {}: {}", file.filename(), e.getMessage());
            }
        }
    }
}