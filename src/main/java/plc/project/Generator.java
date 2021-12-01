package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

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

    private void print(List<Ast.Statement> statements) {
        newline(++indent);
        for(int i = 0; i < statements.size(); i++){
            visit(statements.get(i));
            if( i != statements.size() - 1) newline(indent);
        }
        newline(--indent);
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");

        indent++;

        if (!ast.getGlobals().isEmpty()) newline(0);
        for(Ast.Global globes : ast.getGlobals()){
            newline(indent);
            visit(globes);
        }

        newline(0);
        newline(indent);
        writer.write("public static void main(String[] args) {");
        newline(++indent);
        writer.write("System.exit(new Main().main());");
        newline(--indent);
        writer.write("}");

        for(Ast.Function function : ast.getFunctions()){
            newline(0);
            newline(indent);
            visit(function);
        }

        newline(0);
        newline(--indent);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(!ast.getMutable()) writer.write("final ");
        writer.write(Environment.getType(ast.getTypeName()).getJvmName());
        if(ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList){
            writer.write("[]");
        }
        writer.write( " " + ast.getName());

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
            print(ast.getStatements());
            writer.write("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        writer.write(";");
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
        visit(ast.getReceiver());
        writer.write(" = ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        writer.write("if" + " (");
        visit(ast.getCondition());
        writer.write(")" + " {");

        print(ast.getThenStatements());
        writer.write("}");

        if (ast.getElseStatements().size() != 0){
            writer.write(" else {");
            print(ast.getElseStatements());
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        writer.write("switch (");
        visit(ast.getCondition());
        writer.write(") {");
        newline(++indent);
        for(int i = 0; i < ast.getCases().size(); i++){
            visit(ast.getCases().get(i));
        }
        newline(--indent);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if(ast.getValue().isPresent()){
            writer.write("case ");
            visit(ast.getValue().get());
            writer.write(":");

            print(ast.getStatements());
        } else {
            writer.write("default" );
            writer.write(":");
            // don't use print() because default be built different
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
                if( i != ast.getStatements().size() - 1) newline(indent);
            }
            --indent;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while" + " (");
        visit(ast.getCondition());
        writer.write(")" + " {");

        print(ast.getStatements());
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
        if(ast.getType().getJvmName().equals("String")){
            writer.write("\"" + ast.getLiteral().toString() + "\"");
        } else if (ast.getType().getJvmName().equals("char")){
            writer.write("'" + ast.getLiteral().toString() + "'");
        } else {
            writer.write(ast.getLiteral().toString());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (!ast.getOperator().equals("^")){
            visit(ast.getLeft());
            writer.write(" " + ast.getOperator() + " ");
        } else {
            writer.write("Math.pow(");
            visit(ast.getLeft());
            writer.write(", ");
        }
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (!ast.getOffset().isPresent()){
            writer.write(ast.getName());
        }
        else {
            writer.write(ast.getName() + "[");
            visit(ast.getOffset().get());
            writer.write("]");
        }
        return null;
    }

    //not sure if it should end with semi-colon, example on assignment different than tested
    @Override
    public Void visit(Ast.Expression.Function ast) {
        writer.write(ast.getFunction().getJvmName() + "(");
        for(int i = 0; i < ast.getArguments().size(); i++){
            visit(ast.getArguments().get(i));
            if(i != ast.getArguments().size()-1) writer.write(", ");
        }
        writer.write(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        writer.write("{");
        for(int i = 0; i < ast.getValues().size(); i++){
            visit(ast.getValues().get(i));
            if(i != ast.getValues().size() - 1) writer.write(", ");
        }
        writer.write("}");
        return null;
    }
}
