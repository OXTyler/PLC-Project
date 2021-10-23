package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) return Environment.NIL;
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        if(operator == "&&"){
            Boolean leftSide = requireType(Boolean.class, visit(ast.getRight()));
            Boolean rightSide = requireType(Boolean.class, visit(ast.getRight()));
            return Environment.create(leftSide && rightSide);
        }
        if(operator == "||"){
            Boolean leftSide = requireType(Boolean.class, visit(ast.getRight()));
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
                return Environment.create(leftSide.intValue() + rightSide.intValue());

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(leftSide.doubleValue() + rightSide.doubleValue());

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
                return Environment.create(leftSide.intValue() - rightSide.intValue());

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(leftSide.doubleValue() - rightSide.doubleValue());

            }

        }
        if(operator == "*"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(leftSide.intValue() * rightSide.intValue());

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                return Environment.create(leftSide.doubleValue() * rightSide.doubleValue());

            }

        }
        if(operator == "/"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                if(rightSide.intValue() == 0) throw new RuntimeException("0 in denominator");
                return Environment.create(leftSide.intValue() / rightSide.intValue());

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal rightSide = requireType(BigDecimal.class, visit(ast.getRight()));
                if(rightSide.doubleValue() == 0.0) throw new RuntimeException("0 in denominator");
                BigDecimal value = new BigDecimal((leftSide.doubleValue() / rightSide.doubleValue())).setScale(1, RoundingMode.HALF_EVEN);
                return Environment.create(value);

            }

        }
        if(operator == "^"){
            if(BigInteger.class.isInstance(visit(ast.getLeft()).getValue())){

                BigInteger leftSide = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                return Environment.create(leftSide.intValue() ^ rightSide.intValue());

            } else if(BigDecimal.class.isInstance(visit(ast.getLeft()).getValue())){

                BigDecimal leftSide = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigInteger rightSide = requireType(BigInteger.class, visit(ast.getRight()));
                BigDecimal base = leftSide;
                for(int i = 0; i < rightSide.intValue(); i++){
                    leftSide = base * leftSide;
                }
                return Environment.create(leftSide.doubleValue() ^ rightSide.intValue());

            }

        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
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
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
