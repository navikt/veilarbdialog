package no.nav.fo.veilarbdialog.db.testdriver;

import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

import static no.nav.fo.veilarbdialog.db.testdriver.HsqlSyntaxMapper.hsqlSyntax;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ClassUtils.getMethod;

public class StatementInvocationHandler implements InvocationHandler {

    private static final Method EXECUTE_METHOD = getMethod(Statement.class, "execute", String.class);
    private static final Logger LOG = getLogger(StatementInvocationHandler.class);

    private final Statement statement;

    StatementInvocationHandler(Statement statement) {
        this.statement = statement;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (EXECUTE_METHOD.equals(method)) {
            args[0] = hsqlSyntax((String) args[0]);
            LOG.info("{}", args[0]);
        }
        return method.invoke(statement, args);
    }

}
