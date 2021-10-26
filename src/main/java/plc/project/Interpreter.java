package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for(int i =0; i < ast.getGlobals().size(); i++){
            visit(ast.getGlobals().get(i));
        }
        if(ast.getFunctions().get(0).getName() == "main" && ast.getFunctions().get(0).getParameters().size() == 0){
            return visit(ast.getFunctions().get(0));
        }
        throw new RuntimeException("missing main function");
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().equals(Optional.empty())){
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        } else {
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope functionScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope prevScope = scope;
            scope = new Scope(functionScope);
            try {
                List<String> parameters = ast.getParameters();

                for(int i = 0; i < parameters.size(); i++) {
                    scope.defineVariable(parameters.get(i), true, args.get(i));
                }

                ast.getStatements().forEach(this::visit);
            } catch(Return r) {
                return r.value;
            } finally {
                scope = prevScope;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()){
            scope.defineVariable(
                    ast.getName(),
                    true,
                    visit(ast.getValue().get()));
        } else {
            scope.defineVariable(
                    ast.getName(),
                    true,
                    Environment.NIL
            );
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression.Access var = requireType(Ast.Expression.Access.class, Environment.create(ast.getReceiver()));
        scope.lookupVariable(var.getName()).setValue(visit(ast.getValue()));
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        List<Ast.Statement> statements;
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            statements = ast.getThenStatements();
        } else {
            statements = ast.getElseStatements();
        }

        try {
            scope = new Scope(scope);
            statements.forEach(this::visit);
        } finally {
            scope = scope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Object condition = visit(ast.getCondition()).getValue();
        for (Ast.Statement.Case c : ast.getCases()) {
            // if case matches condition or is default case (this assumes that default case is always last)
            if (!c.getValue().isPresent() || condition.equals(visit(c).getValue())) {
                try {
                    scope = new Scope(scope);
                    c.getStatements().forEach(this::visit);
                } finally {
                    scope = scope.getParent();
                }
                break;
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        return visit(ast.getValue().get());
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while( requireType(Boolean.class, visit( ast.getCondition()))) {
            try{
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) return Environment.NIL;
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Object o = visit(ast.getLeft()).getValue();
        if(operator == "&&"){
            Boolean leftSide = requireType(Boolean.class, visit(ast.getLeft()));
            Boolean rightSide = requireType(Boolean.class, visit(ast.getRight()));
            return Environment.create(leftSide && rightSide);
        }
        if(operator == "||"){
            Boolean leftSide = requireType(Boolean.class, visit(ast.getLeft()));
            if(leftSide) return Environment.create(leftSide);
            Boolean rightSide = requireType(Boolean.class, visit(ast.getRight()));
            return Environment.create(rightSide);

        }
        if(operator == "<"){
            Comparable leftSide = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rightSide = requireType(Comparable.class, visit(ast.getRight()));
            if(leftSide.compareTo(rightSide) < 0) return Environment.create(true);
            return Environment.create(false);
        }
        if(operator == ">"){
            Comparable leftSide = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rightSide = requireType(Comparable.class, visit(ast.getRight()));
            if(leftSide.compareTo(rightSide) > 0) return Environment.create(true);
            return Environment.create(false);
        }
        if(operator == "=="){
            Comparable leftSide = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rightSide = requireType(Comparable.class, visit(ast.getRight()));
            return Environment.create(leftSide.equals(rightSide));
        }
        if(operator == "!="){
            Comparable leftSide = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rightSide = requireType(Comparable.class, visit(ast.getRight()));
            return Environment.create(!leftSide.equals(rightSide));
        }
        if(operator == "+"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(leftSide.add(rightSide));

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(BigDecimal.valueOf(leftSide.doubleValue() + rightSide.doubleValue()));

            } else if(String.class.isInstance(visit(ast.getLeft()).getValue())){

                String leftSide = requireType(String.class, visit(ast.getLeft()));
                String rightSide = requireType(String.class, visit(ast.getRight()));
                return Environment.create(leftSide + rightSide);

            }


        }
        if(operator == "-"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(BigInteger.valueOf(leftSide.intValue() - rightSide.intValue()));

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(leftSide.subtract(rightSide));
            }

        }
        if(operator == "*"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(BigInteger.valueOf(leftSide.intValue() * rightSide.intValue()));

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(BigDecimal.valueOf(leftSide.doubleValue() * rightSide.doubleValue()));

            }

        }
        if(operator == "/"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                if(rightSide.intValue() == 0) throw new RuntimeException("0 in denominator");
                return Environment.create(BigInteger.valueOf(leftSide.intValue() / rightSide.intValue()));

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                if(rightSide.doubleValue() == 0.0) throw new RuntimeException("0 in denominator");
                BigDecimal value = new BigDecimal((leftSide.doubleValue() / rightSide.doubleValue())).setScale(1, RoundingMode.HALF_EVEN);
                return Environment.create(value);

            }

        }
        if(operator == "^") {
            if (BigInteger.class.isInstance(visit(ast.getLeft()).getValue())) {

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(findPow(leftSide, rightSide));
            } else if (BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())) {

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(findPow(leftSide, rightSide));
            }
        }
        return Environment.NIL;
    }

    BigInteger findPow(BigInteger base, BigInteger pow){
        BigInteger val = base;
        BigInteger iter = new BigInteger("0");
        while(!pow.subtract(iter).equals(new BigInteger("1"))){
            val = val.multiply(base);
            iter = iter.add(BigInteger.ONE);
        }
        return val;
    }

    BigDecimal findPow(BigDecimal base, BigInteger pow){
        BigDecimal val = base;
        BigInteger iter = new BigInteger("0");
        while(!pow.subtract(iter).equals(new BigInteger("1"))){
            val = val.multiply(base);
            iter = iter.add(BigInteger.ONE);
        }
        return val;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()) {
            List o = (List) scope.lookupVariable(ast.getName()).getValue().getValue();
            int i = ((BigInteger)((Ast.Expression.Literal)ast.getOffset().get()).getLiteral()).intValue();
            return Environment.create(o.get(i));
        } else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        ArrayList<Environment.PlcObject> args = new ArrayList<>();
        ast.getArguments().forEach(expression -> args.add(visit(expression)));

        return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    public static class Return extends RuntimeException {

        public final Environment.PlcObject value;

        public Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
