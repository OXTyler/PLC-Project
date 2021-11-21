package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(ast.getTypeName().isPresent()){
            writer.write(Environment.getType(ast.getTypeName().get()).getJvmName() + " " + ast.getName());
            if(ast.getValue().isPresent()) {
                writer.write(" = ");
                visit(ast.getValue().get());
            }
        }
        else{
            String type =  ast.getValue().get().getType().getJvmName();
            writer.write(type + " " + ast.getName() + " = ");
            visit(ast.getValue().get());
        }

        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) { //TODO: probably broke tbh
        writer.write("if" + "(" + ast.getCondition() + ")" + " {");
        newline(1);
        for(Ast.Statement then : ast.getThenStatements()){
            visit(then);
            newline(1);
        }
        writer.write("}");
        if(ast.getElseStatements().size() != 0){
            writer.write("else {");
            newline(1);
            for(Ast.Statement Else : ast.getElseStatements()){
                visit(Else);
                newline(1);
            }
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) { //TODO: havent tested yet
        writer.write("while" + " (" + ast.getCondition() + ")" + " {");
        newline(1);
        for(Ast.Statement state : ast.getStatements()){
            writer.write(state.toString() + ";");
            newline(1);
        }
        newline(0);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return " + ast.getValue() + ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) { //TODO: consider BigDecimal
        if(ast.getLiteral().getClass().isInstance(String.class)){
            writer.write("\"" + ast.getLiteral().toString() + "\"");
        }
        else if(ast.getLiteral().getClass().isInstance(Character.class)){
            writer.write("'" + ast.getLiteral().toString() + "'");
        }
        else{
            writer.write(ast.getLiteral().toString());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }



}
