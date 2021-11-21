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
        this.indent = indent;
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");
        newline(indent);
        newline(indent + 1);
        for(Ast.Global globes : ast.getGlobals()){
            visit(globes);
            newline(indent);
        }
        writer.write("public static void main(String[] args) {");
        newline(indent + 1);
        writer.write("System.exit(new Main().main());");
        newline(indent - 1);
        writer.write("}");

        newline(0);
        newline(indent + 1);
        for(Ast.Function function : ast.getFunctions()){
            visit(function);
        }
        newline(indent - 1);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(!ast.getMutable()) writer.write("final ");
        writer.write(Environment.getType(ast.getTypeName()).getJvmName() + " " + ast.getName());

        if(ast.getValue().isPresent()){
            writer.write(" = ");
            visit(ast.getValue().get());
        }
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        if(ast.getReturnTypeName().isPresent()){
            writer.write(Environment.getType(ast.getReturnTypeName().get()).getJvmName() + " ");
            writer.write(ast.getName() + "(");
            for(int i = 0; i < ast.getParameters().size(); i++){
                writer.write(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName() + " " + ast.getParameters().get(i));
                if(i != ast.getParameters().size() - 1) writer.write(", ");
            }
            writer.write(")" + " {");
            newline(indent + 1);
            for(Ast.Statement states: ast.getStatements()){
                visit(states);
            }
            newline(indent - 1);
            writer.write("}");
            newline(indent - 1);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        newline(indent);
        return null;
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
        newline(indent - 1);
        for(Ast.Statement state : ast.getThenStatements()){
            visit((state));
        }
        writer.write("}");
        if(ast.getElseStatements().size() != 0){
            writer.write("else {");
            newline(indent);
            for(Ast.Statement state : ast.getElseStatements()){
                visit((state));
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
        newline(indent + 1);
        for(Ast.Statement state : ast.getStatements()){
            visit(state);
            newline(indent);
        }
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) { //TODO: consider BigDecimal
        if(ast.getType().getJvmName() == "String"){
            writer.write("\"" + ast.getLiteral().toString() + "\"");
        }
        else if(ast.getType().getJvmName() == "Character"){
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

    //not sure if it should end with semi-colon, example on assignment different than tested
    @Override
    public Void visit(Ast.Expression.Function ast) {
        writer.write(ast.getFunction().getJvmName() + "(");
        for(int i = 0; i < ast.getArguments().size(); i++){
            visit(ast.getArguments().get(i));
            if(i != ast.getArguments().size()-1) writer.write(", ");
        }
        writer.write(");");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }



}
