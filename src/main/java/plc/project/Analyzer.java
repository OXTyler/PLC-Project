package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        boolean found = false;
        for(Ast.Global globals : ast.getGlobals()){
            visit(globals);
        }
        for(Ast.Function func : ast.getFunctions()){
            if(func.getName() == "main"){
                found = true;
                if(func.getReturnTypeName().get() != "Integer"){
                    throw new RuntimeException("main does not return Integer");
                }
                for(Ast.Statement states : func.getStatements()){
                    if((states instanceof Ast.Statement.Return)){
                        if(!(((Ast.Expression.Literal) ((Ast.Statement.Return) states).getValue()).getLiteral() instanceof  Integer)){
                            throw new RuntimeException("invalid Return");
                        }
                    }
                }
            }
            visit(func);
        }
        if(!found) throw new RuntimeException("missing main function");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
       if(ast.getValue().isPresent()) {
           visit(ast.getValue().get());
           requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
           ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), ast.getValue().get().getType(), true, Environment.NIL));
           scope.defineVariable(ast.getName(), ast.getName(), ast.getVariable().getType(), true, Environment.create(ast.getVariable()));
       }
       else{
           ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), true, Environment.NIL));
           scope.defineVariable(ast.getName(), ast.getName(), ast.getVariable().getType(), true, Environment.create(ast.getVariable()));
       }
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        ArrayList<Environment.Type> parameterTypes = new ArrayList<>();
        for(String parameter: ast.getParameterTypeNames()){
            parameterTypes.add(Environment.getType(parameter));
        }
        Environment.Function func;
        if(ast.getReturnTypeName().isPresent()){
            func = scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, Environment.getType(ast.getReturnTypeName().get()), args -> Environment.NIL);
        } else {
            func = scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, Environment.getType("Nil"), args -> Environment.NIL);
        }
        ast.setFunction(func);
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Function expression required.");
        }

        visit(ast.getExpression());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) throw new RuntimeException("Type Unknown");

        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if(ast.getTypeName().isPresent()) {
                requireAssignable(Environment.getType(ast.getTypeName().get()), ast.getValue().get().getType());
            }
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), ast.getValue().get().getType(), true, Environment.NIL));
            scope.defineVariable(ast.getName(), ast.getName(), ast.getVariable().getType(), true, Environment.create(ast.getVariable()));
        }
        else{
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()), true, Environment.NIL));
            scope.defineVariable(ast.getName(), ast.getName(), ast.getVariable().getType(), true, Environment.create(ast.getVariable()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Receiver needs to be an access expression to be assignable");
        }

        visit(ast.getReceiver());
        visit(ast.getValue());

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("then statements cannot be empty");
        }

        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        ast.getCondition().getType();
        if(ast.getCases().get(ast.getCases().size() - 1).getValue().isPresent()) throw new RuntimeException("Last case has value");
        for(Ast.Statement.Case Case : ast.getCases()){
            scope = new Scope(scope);
            visit(Case);
            if(Case.getValue().isPresent()){
                requireAssignable(ast.getCondition().getType(), Case.getValue().get().getType());
            }
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
       for(Ast.Statement state : ast.getStatements()){
           visit(state);
       }
       if(ast.getValue().isPresent()) visit(ast.getValue().get());
       return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
        } else if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (ast.getLiteral() instanceof BigInteger) {
            if (((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1 ||
                    ((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) == -1) {
                throw new RuntimeException("value is out of range");
            }

            ast.setType(Environment.Type.INTEGER);
        } else if (ast.getLiteral() instanceof BigDecimal) {
            if (((BigDecimal) ast.getLiteral()).compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 1 ||
                    ((BigDecimal) ast.getLiteral()).compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) == -1) {
                throw new RuntimeException("value is out of range");
            }

            ast.setType(Environment.Type.DECIMAL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Expression must be a binary binary expression");
        }

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());

        Environment.Type lType = ast.getLeft().getType();
        Environment.Type rType = ast.getRight().getType();

        switch (ast.getOperator()) {
            case "&&":
            case "||":
                requireAssignable(Environment.Type.BOOLEAN, lType);
                requireAssignable(Environment.Type.BOOLEAN, rType);
                ast.setType(Environment.Type.BOOLEAN);

                break;
            case "<":
            case ">":
            case "==":
            case "!=":
                requireAssignable(Environment.Type.COMPARABLE, lType);
                requireAssignable(Environment.Type.COMPARABLE, rType);

                if (lType != rType) {
                    throw new RuntimeException("Operands must be the same type");
                }

                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (lType == Environment.Type.STRING || rType == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                } else if (!((rType == Environment.Type.INTEGER || rType == Environment.Type.DECIMAL) && rType == lType)) {
                    throw new RuntimeException("Operand types must be both either Integer or Decimal");
                }

                ast.setType(lType);
                break;
            case "-":
            case "*":
            case "/":
                if (!((rType == Environment.Type.INTEGER || rType == Environment.Type.DECIMAL) && rType == lType)) {
                    throw new RuntimeException("Operand types must be both either Integer or Decimal");
                }

                ast.setType(lType);
                break;
            case "^":
                requireAssignable(Environment.Type.INTEGER, rType);
                if (lType != Environment.Type.INTEGER && lType != Environment.Type.DECIMAL) {
                    throw new RuntimeException("LHS must be either an Integer or a Decimal");
                }

                ast.setType(lType);
                break;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent() && ast.getOffset().get().getType() != Environment.Type.INTEGER) {
            throw new RuntimeException("Offset type must be INTEGER");
        }

        Environment.Variable var = getScope().lookupVariable(ast.getName());
        ast.setVariable(var);

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        ast.setFunction(function);

        for (int i = 0; i < ast.getArguments().size(); i++) {
            Ast.Expression arg = ast.getArguments().get(i);
            visit(arg);

            requireAssignable(ast.getFunction().getParameterTypes().get(i), arg.getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        visit(ast.getValues().get(0));
        ast.setType(ast.getValues().get(0).getType());
        for(Ast.Expression val : ast.getValues()){
            visit(val);
            requireAssignable(ast.getType(), val.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target == Environment.Type.ANY) return;
        if (target == Environment.Type.COMPARABLE && (type == Environment.Type.INTEGER || type == Environment.Type.DECIMAL || type == Environment.Type.CHARACTER || type == Environment.Type.STRING)) return;
        if (target != type) {
            throw new RuntimeException(type.getName() + " cannot be assigned to " + target.getName());
        }
    }

}
