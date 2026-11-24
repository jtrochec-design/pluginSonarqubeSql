package bo.gob.gestora.sonar.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.parser.ASTNodeAccess;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class SqlAstVisitor extends StatementVisitorAdapter {
    private final SensorContext context;
    private final InputFile inputFile;
    private int dmlCount = 0;

    public SqlAstVisitor(SensorContext context, InputFile inputFile) {
        this.context = context;
        this.inputFile = inputFile;
    }

    public int getDmlCount() {
        return dmlCount;
    }

    private void reportIssue(String ruleId, int line, String message) {
        int validLine = (line > 0 && line <= inputFile.lines()) ? line : 1;
        NewIssue newIssue = context.newIssue()
                .forRule(RuleKey.of(SqlRulesDefinition.REPOSITORY_KEY, ruleId));
        NewIssueLocation location = newIssue.newLocation()
                .on(inputFile)
                .at(inputFile.selectLine(validLine))
                .message(message);
        newIssue.at(location).save();
    }

    private int extractLine(Object statement) {
        if (statement instanceof ASTNodeAccess access
                && access.getASTNode() != null
                && access.getASTNode().jjtGetFirstToken() != null) {
            return access.getASTNode().jjtGetFirstToken().beginLine;
        }
        return 1;
    }

    @Override
    public void visit(Select select) {
        if (select.getSelectBody() instanceof PlainSelect plainSelect) {
            int line = extractLine(select);

         // SQL_001: Proyección Indiscriminada (SELECT *)
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                Expression expr = item.getExpression();
                if (expr instanceof AllColumns || expr instanceof AllTableColumns) {
                    reportIssue("SQL_001", line, "Antipatrón detectado: Evite usar 'SELECT *'. Proyecte columnas explícitas.");
                }
            }

            // SQL_002: Función Escalar en Predicado (Non-Sargable)
            Expression where = plainSelect.getWhere();
            if (where != null) {
                where.accept(new ExpressionVisitorAdapter() {
                    @Override
                    public void visit(Function function) {
                        if (function.getParameters() != null && function.getParameters().getExpressions() != null) {
                            for (Object param : function.getParameters().getExpressions()) {
                                if (param instanceof Column) {
                                    reportIssue("SQL_002", line, "Predicado Non-Sargable: Evite aplicar la función '" + function.getName() + "' sobre una columna en WHERE.");
                                }
                            }
                        }
                        super.visit(function);
                    }
                });
            }
        }
    }

    @Override
    public void visit(Update update) {
        dmlCount++;
        // SQL_003: UPDATE sin WHERE
        if (update.getWhere() == null) {
            reportIssue("SQL_003", extractLine(update), "Riesgo de Integridad: Sentencia UPDATE sin cláusula WHERE.");
        }
    }

    @Override
    public void visit(Delete delete) {
        dmlCount++;
        // SQL_003: DELETE sin WHERE
        if (delete.getWhere() == null) {
            reportIssue("SQL_003", extractLine(delete), "Riesgo de Integridad: Sentencia DELETE sin cláusula WHERE.");
        }
    }

    @Override
    public void visit(Insert insert) {
        dmlCount++;
    }

    @Override
    public void visit(Execute execute) {
        int line = extractLine(execute);
        // SQL_004: Inyección SQL (Concatenación dinámica en EXEC)
        if (execute.getExprList() != null && execute.getExprList().getExpressions() != null) {
            for (Object obj : execute.getExprList().getExpressions()) {
                if (obj instanceof Expression expr) {
                    expr.accept(new ExpressionVisitorAdapter() {
                        @Override
                        public void visit(Addition addition) {
                            reportIssue("SQL_004", line, "Vulnerabilidad SQLi: Concatenación dinámica (+) detectada en ejecución.");
                        }

                        @Override
                        public void visit(Concat concat) {
                            reportIssue("SQL_004", line, "Vulnerabilidad SQLi: Concatenación dinámica (||) detectada en ejecución.");
                        }
                    });
                }
            }
        }
    }
}