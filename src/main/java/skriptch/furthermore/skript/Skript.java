package skriptch.furthermore.skript;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Skript {
	static class Context { 
		public Context parent;
		public Map<String, Value> vars = new HashMap<String, Value>();
		public Context() {}
		public Context(Context parent) {
			this.parent = parent;
		}
		public void put(String name, Value value) {
			if (!replace(name, value)) {
				vars.put(name, value);
			}
		}
		public boolean replace(String name, Value value) {
			if (vars.containsKey(name)) {
				vars.put(name, value);
				return true;
			}
			else {
				if (parent != null) {
					return parent.replace(name, value);
				}
				else {
					return false;
				}
			}
		}
		public Value get(String name) {
			if (vars.containsKey(name)) {
				return vars.get(name);
			}
			else if (parent == null) {
				return null;
			}
			else {
				return parent.get(name);
			}
		}
	}
	
	static class Tokenizer {
		private Scanner st;
		private LinkedList<String> pushedBack = new LinkedList<String>();
		public Tokenizer(String s) {
			st = new Scanner(s);
		}
		public boolean hasMore() {
			if (!pushedBack.isEmpty()) return true;
			
			String next = st.nextToken();
			if (next != null) {
				pushback(next);
			}
			
			return next != null;
		}
		public String next() {
			if (!pushedBack.isEmpty()) {
				return pushedBack.removeFirst();
			}
			else {
				return st.nextToken();
			}
		}
		
		public void pushback(String token) {
			pushedBack.addFirst(token);
		}
	}
	
	interface IEvalInContext {
		public Value evalInContext(Context global, Context ctx);
	}
	
	static class Block implements IEvalInContext {
		public List<Statement> statements = new LinkedList<Statement>();
		
		public static Block parse(Tokenizer t) {
			Block block = new Block();
			
			if (!"{".equals(t.next())) throw new RuntimeException("{ expected");
			
			for (;;) {
				String token = t.next();
				
				if ("}".equals(token)) break;
				
				t.pushback(token);
				block.statements.add(Statement.parse(t));
			
				token = t.next();
				if ("}".equals(token)) break;
				if (!";".equals(token)) throw new RuntimeException("; expected, got: " + token);
			}
			
			return block;
		}
		
		public Value eval(Context global, Context ctx) {
			Context blockCtx = new Context(ctx);
			return evalInContext(global, blockCtx);
		}
		
		public Value evalInContext(Context global, Context blockCtx) {
			Value last = null;
			for (Statement s : statements) {
				last = s.eval(global, blockCtx);
			}
			return last;
		}
	}
	
	static abstract class Statement {
		public static Statement parse(Tokenizer t) {
			String token = t.next();
			
			t.pushback(token);
			
			if ("if".equals(token)) {
				return IfStatement.parse(t);
			}
			else if ("for".equals(token)) {
				return ForStatement.parse(t);
			}
			else if ("def".equals(token)) {
				return FunctionDefinitionStatement.parse(t);
			}
			else {
				token = t.next();
				String token2 = t.next();
				t.pushback(token2);
				t.pushback(token);
				
				if ("(".equals(token2)) {
					return FunctionCall.parse(t);
				}
				else return AssignmentStatement.parse(t);
			}
		}
		
		public abstract Value eval(Context global, Context ctx);
	}
	
	static class FunctionDefinitionStatement extends Statement {
		public String variable;
		public List<String> params = new LinkedList<String>();
		public Block block;
		
		public static FunctionDefinitionStatement parse(Tokenizer t) {
			FunctionDefinitionStatement fds = new FunctionDefinitionStatement();
			
			String token = t.next();
			if (!"def".equals(token)) throw new RuntimeException("def expected, got: " + token);
			
			fds.variable = t.next();
			
			token = t.next();
			if ("(".equals(token)) {
				for (;;) {
					fds.params.add(t.next());
					
					token = t.next();
					if (")".equals(token)) {
						break;
					}
					
					if (!",".equals(token)) throw new RuntimeException(", expected, got: " + token);
				}
			}
			else {
				t.pushback(token);
			}
			
			fds.block = Block.parse(t);
			
			return fds;
		}
		
		public Value eval(Context global, Context ctx) {
			FunctionValue blockValue = new FunctionValue(params, block);
			if (isGlobal(variable)) {
				global.put(variable, blockValue);
			}
			else {
				ctx.put(variable, blockValue);
			}
			return blockValue;
		}
	}
	
	static class AssignmentStatement extends Statement {
		public String variable;
		public Expression expression;
		
		public static AssignmentStatement parse(Tokenizer t) {
			AssignmentStatement as = new AssignmentStatement();
			
			as.variable = t.next();
			
			String token = t.next();
			if (!":=".equals(token)) throw new RuntimeException(":= expected, got: " + token);
			
			as.expression = Expression.parse(t);
			
			return as;
		}
		
		public Value eval(Context global, Context ctx) {
			Value value = expression.eval(global, ctx);
			if (isGlobal(variable)) {
				global.put(variable, value);
			}
			else {
				ctx.put(variable, value);
			}
			return value;
		}
	}
	
	private static boolean isGlobal(String name) {
		return Character.isUpperCase(name.charAt(0));
	}		
	
	static class ForStatement extends Statement {
		public AssignmentStatement init;
		public Expression condition;
		public AssignmentStatement increment;
		public Block whileBlock;
		
		public static ForStatement parse(Tokenizer t) {
			ForStatement fs = new ForStatement();
					
			String token = t.next();
			if (!"for".equals(token)) throw new RuntimeException("for expected, got: " + token);
			
			fs.init = AssignmentStatement.parse(t);
			
			token = t.next();
			if (!";".equals(token)) throw new RuntimeException("; expected, got: " + token);
			
			fs.condition = Expression.parse(t);
			
			token = t.next();
			if (!";".equals(token)) throw new RuntimeException("; expected, got: " + token);
			
			fs.increment = AssignmentStatement.parse(t);
			
			fs.whileBlock = Block.parse(t);
			
			return fs;
		}
		
		public Value eval(Context global, Context ctx) {
			Context blockCtx = new Context(ctx);
			
			init.eval(global, blockCtx);
			
			Value last = null;
			while (((IntegerValue) condition.eval(global, blockCtx)).value == 1) {
				last = whileBlock.evalInContext(global, blockCtx);
				
				increment.eval(global, blockCtx);
			}
			return last;
		}
	}
	
	static class IfStatement extends Statement {
		public Expression expression;
		public Block thenBlock;
		public Block elseBlock;
		
		public static IfStatement parse(Tokenizer t) {
			IfStatement is = new IfStatement();
			
			String token = t.next();
			if (!"if".equals(token)) throw new RuntimeException("if expected, got: " + token);
			
			is.expression = Expression.parse(t);
			
			token = t.next();
			if (!"then".equals(token)) throw new RuntimeException("then expected, got: " + token);
			
			is.thenBlock = Block.parse(t);
			
			token = t.next();
			if ("else".equals(token)) {
				is.elseBlock = Block.parse(t);
			}
			else {
				t.pushback(token);
			}
			
			return is;
		}

		public Value eval(Context global, Context ctx) {
			if (((IntegerValue) expression.eval(global, ctx)).value == 1) {
				return thenBlock.eval(global, ctx);
			}
			else if (elseBlock != null) {
				return elseBlock.eval(global, ctx);
			}
			else throw new RuntimeException("if without then or else");
		}
	}
	
	static class Expression {
		public List<RelationalOperatorSimpleExpression> relationalOperatorSimpleExpressions = new LinkedList<RelationalOperatorSimpleExpression>();

		public static Expression parse(Tokenizer t) {
			Expression expression = new Expression();
			
			RelationalOperatorSimpleExpression relationalOperatorSimpleExpression = new RelationalOperatorSimpleExpression();
			relationalOperatorSimpleExpression.simpleExpression = SimpleExpression.parse(t);;
			expression.relationalOperatorSimpleExpressions.add(relationalOperatorSimpleExpression);
			
			while (t.hasMore()) {
				String token = t.next();
				
				if (!("<".equals(token) || ">".equals(token) || "=".equals(token))) {
					t.pushback(token);
					break;
				}
				
				relationalOperatorSimpleExpression = new RelationalOperatorSimpleExpression();
				relationalOperatorSimpleExpression.relationalOperator = token;
				relationalOperatorSimpleExpression.simpleExpression = SimpleExpression.parse(t);;
				expression.relationalOperatorSimpleExpressions.add(relationalOperatorSimpleExpression);
			}
			
			return expression;
		}
		
		public Value eval(Context global, Context ctx) {
			RelationalOperatorSimpleExpression last = null;
			Value result = null;
			for (RelationalOperatorSimpleExpression rose : relationalOperatorSimpleExpressions) {
				if (last == null) {
					if (rose.relationalOperator != null) throw new RuntimeException("Unknown relationalOperator=" + rose.relationalOperator);
					result = rose.simpleExpression.eval(global, ctx);
				}
				else {
					if ("=".equals(rose.relationalOperator)) {
						result = result.eq(rose.simpleExpression.eval(global, ctx));
					}
					else if (">".equals(rose.relationalOperator)) {
						result = result.gt(rose.simpleExpression.eval(global, ctx));
					}
					else if ("<".equals(rose.relationalOperator)) {
						result = result.lt(rose.simpleExpression.eval(global, ctx));
					}
					else throw new RuntimeException("Unknown relationalOperator=" + rose.relationalOperator);
				}
				
				last = rose;
			}
			return result;
		}
	}
	
	static class RelationalOperatorSimpleExpression {
		public String relationalOperator;
		public SimpleExpression simpleExpression;
	}
	
	static class SimpleExpression {
		public List<AdditionOperatorTerm> additionOperatorTerms = new LinkedList<AdditionOperatorTerm>();

		public static SimpleExpression parse(Tokenizer t) {
			SimpleExpression simpleExpression = new SimpleExpression();
			
			AdditionOperatorTerm additionOperatorTerm = new AdditionOperatorTerm();
			additionOperatorTerm.term = Term.parse(t);
			simpleExpression.additionOperatorTerms.add(additionOperatorTerm);
			
			while (t.hasMore()) {
				String token = t.next();
				
				if (!("+".equals(token) || "-".equals(token) || "OR".equals(token))) {
					t.pushback(token);
					break;
				}
				
				additionOperatorTerm = new AdditionOperatorTerm();
				additionOperatorTerm.additionOperator = token;
				additionOperatorTerm.term = Term.parse(t);
				simpleExpression.additionOperatorTerms.add(additionOperatorTerm);
			}
			
			return simpleExpression;
		}
		
		public Value eval(Context global, Context ctx) {
			AdditionOperatorTerm last = null;
			Value result = null;
			for (AdditionOperatorTerm aot : additionOperatorTerms) {
				if (last == null) {
					if (aot.additionOperator != null) throw new RuntimeException("Unexpected additionOperator=" + aot.additionOperator);
					result = aot.term.eval(global, ctx);
				}
				else {
					if ("+".equals(aot.additionOperator)) {
						result = result.add(aot.term.eval(global, ctx));
					}
					else if ("-".equals(aot.additionOperator)) {
						result = result.sub(aot.term.eval(global, ctx));
					}
					else if ("OR".equals(aot.additionOperator)) {
						result = result.or(aot.term.eval(global, ctx));
					}
					else throw new RuntimeException("Unknown additionOperator=" + aot.additionOperator);
				}
				
				last = aot;
			}
			return result;
		}
	}
	
	static class AdditionOperatorTerm {
		public String additionOperator;
		public Term term;
	}
	
	static class Term {
		public List<MultiplicationOperatorFactor> multiplicationOperatorFactors = new LinkedList<MultiplicationOperatorFactor>();

		public static Term parse(Tokenizer t) {
			Term term = new Term();
			
			MultiplicationOperatorFactor multiplicationOperatorFactor = new MultiplicationOperatorFactor();
			multiplicationOperatorFactor.factor = Factor.parse(t);
			term.multiplicationOperatorFactors.add(multiplicationOperatorFactor);
			
			while (t.hasMore()) {
				String token = t.next();
				
				if (!("/".equals(token) || "*".equals(token) || "&".equals(token))) {
					t.pushback(token);
					break;
				}
				
				multiplicationOperatorFactor = new MultiplicationOperatorFactor();
				multiplicationOperatorFactor.multiplicationOperator = token;
				multiplicationOperatorFactor.factor = Factor.parse(t);
				term.multiplicationOperatorFactors.add(multiplicationOperatorFactor);
			}
			
			return term;
		}
		
		public Value eval(Context global, Context ctx) {
			MultiplicationOperatorFactor last = null;
			Value result = null;
			for (MultiplicationOperatorFactor mof : multiplicationOperatorFactors) {
				if (last == null) {
					if (mof.multiplicationOperator != null) throw new RuntimeException("Unexpected multiplicationOperator=" + mof.multiplicationOperator);
					result = mof.factor.eval(global, ctx);
				}
				else {
					if ("*".equals(mof.multiplicationOperator)) {
						result = result.mul(mof.factor.eval(global, ctx));
					}
					else if ("/".equals(mof.multiplicationOperator)) {
						result = result.div(mof.factor.eval(global, ctx));
					}
					else if ("&".equals(mof.multiplicationOperator)) {
						result = result.and(mof.factor.eval(global, ctx));
					}
					else throw new RuntimeException("Unknown multiplicationOperator=" + mof.multiplicationOperator);
				}
				
				last = mof;
			}
			return result;
		}
	}
	
	static class MultiplicationOperatorFactor {
		public String multiplicationOperator;
		public Factor factor;
	}
	
	static class Factor {
		public Number number;               //XOR
		public StringLiteral stringLiteral; //XOR
		public Expression expression; 		//XOR
		public Variable variable;	  		//XOR
		public FunctionCall call;			//XOR

		public static Factor parse(Tokenizer t) {
			Factor factor = new Factor();
			
			String token = t.next();
			
			if ("(".equals(token)) {
				factor.expression = Expression.parse(t);
				
				if (!")".equals(t.next())) throw new RuntimeException(") expected");
			}
			else if (token.startsWith("'")) {
				factor.stringLiteral = StringLiteral.parse(token); //FIXME (Tokenizer) string with spaces and "'" escaping
			}
			else {
				String token2 = t.next();
				t.pushback(token2);
				
				if ("(".equals(token2)) {
					t.pushback(token);
					
					factor.call = FunctionCall.parse(t);
				}
				else {
					try {
						factor.number = Number.parse(token);
					}
					catch (NumberFormatException e) {
						factor.variable = Variable.parse(token);
					}
				}
			}
			
			return factor;
		}
		
		public Value eval(Context global, Context ctx) {
			if (number != null) {
				return number.eval(global, ctx);
			}
			else if (stringLiteral != null) {
				return stringLiteral.eval(global, ctx);
			}
			else if (expression != null) {
				return expression.eval(global, ctx);
			}
			else if (call != null) {
				return call.eval(global, ctx);
			}
			else if (variable != null) {
				return variable.eval(global, ctx);
			}
			else throw new RuntimeException("invalid Factor");
		}
	}
	
	static class FunctionCall extends Statement {
		public String variable;
		public List<Expression> params = new LinkedList<Expression>();
		
		public static FunctionCall parse(Tokenizer t) {
			FunctionCall fc = new FunctionCall();
		
			fc.variable = t.next();
			
			String token = t.next();
			if (!"(".equals(token)) throw new RuntimeException("( expected, got: " + token);
			
			for (;;) {
				token = t.next();
				if (")".equals(token)) {
					break;
				}
				else {
					t.pushback(token);
				}
				
				fc.params.add(Expression.parse(t));
				
				token = t.next();
				if (")".equals(token)) {
					break;
				}
				
				if (!",".equals(token)) throw new RuntimeException(", expected, got: " + token);
			}
			
			return fc;
		}

		public Value eval(Context global, Context ctx) {
			FunctionValue functionBlock;
			if (isGlobal(variable)) {
				functionBlock = (FunctionValue) global.get(variable);
			}
			else {
				functionBlock = ((FunctionValue) ctx.get(variable));
			}
			
			Context blockCtx = new Context(ctx);
			Iterator<Expression> paramValueI = params.iterator();
			for (String paramName : functionBlock.params) {
				blockCtx.put(paramName, paramValueI.next().eval(global, ctx));
			}
			
			return functionBlock.value.evalInContext(global, blockCtx);
		}
	}
	
	static class Number {
		public int value;

		public static Number parse(String token) {
			Number number = new Number();
			number.value = Integer.parseInt(token);
			
			return number;
		}
		
		public IntegerValue eval(Context global, Context ctx) {
			return new IntegerValue(value);
		}
	}
	
	static class StringLiteral {
		public String value;

		public static StringLiteral parse(String token) {
			StringLiteral s = new StringLiteral();
			s.value = token.substring(1, token.length() - 1);
			
			return s;
		}
		
		public StringValue eval(Context global, Context ctx) {
			return new StringValue(value);
		}
	}
	
	static class Variable {
		public String name;

		public static Variable parse(String token) {
			Variable variable = new Variable();
			variable.name = token;
			
			return variable;
		}
		
		public Value eval(Context global, Context ctx) {
			if (isGlobal(name)) {
				return global.get(name);
			}
			else {
				return ctx.get(name);
			}
		}
	}
	
	static class Value {
		public Value add(Value v) {
			throw new RuntimeException(getClass().getName() + ".add not implemented");
		}
		public Value sub(Value v) {
			throw new RuntimeException(getClass().getName() + ".sub not implemented");
		}
		public Value or(Value v) {
			throw new RuntimeException(getClass().getName() + ".or not implemented");
		}
		public Value div(Value v) {
			throw new RuntimeException(getClass().getName() + ".div not implemented");
		}
		public Value mul(Value v) {
			throw new RuntimeException(getClass().getName() + ".mul not implemented");
		}
		public Value and(Value v) {
			throw new RuntimeException(getClass().getName() + ".and not implemented");
		}
		public Value eq(Value v) {
			throw new RuntimeException(getClass().getName() + ".eq not implemented");
		}
		public Value lt(Value v) {
			throw new RuntimeException(getClass().getName() + ".lt not implemented");
		}
		public Value gt(Value v) {
			throw new RuntimeException(getClass().getName() + ".gt not implemented");
		}
		public String toString() {
			throw new RuntimeException(getClass().getName() + ".toString not implemented");
		}
	}
	
	static class IntegerValue extends Value {
		public int value;
		public IntegerValue(int v) {
			this.value = v;
		}
		public Value add(Value v) {
			return new IntegerValue(this.value + ((IntegerValue) v).value);
		}
		public Value sub(Value v) {
			return new IntegerValue(this.value - ((IntegerValue) v).value);
		}
		public Value or(Value v) {
			return new IntegerValue(this.value == 1 || ((IntegerValue) v).value == 1 ? 1 : 0);
		}
		public Value div(Value v) {
			return new IntegerValue(this.value / ((IntegerValue) v).value);
		}
		public Value mul(Value v) {
			return new IntegerValue(this.value * ((IntegerValue) v).value);
		}
		public Value and(Value v) {
			return new IntegerValue(this.value == 1 && ((IntegerValue) v).value == 1 ? 1 : 0);
		}
		public Value eq(Value v) {
			return new IntegerValue(this.value == ((IntegerValue) v).value ? 1 : 0);
		}
		public Value lt(Value v) {
			return new IntegerValue(this.value < ((IntegerValue) v).value ? 1 : 0);
		}
		public Value gt(Value v) {
			return new IntegerValue(this.value > ((IntegerValue) v).value ? 1 : 0);
		}
		public String toString() {
			return "" + value;
		}
	}
	
	static class StringValue extends Value {
		public String value;
		public StringValue(String v) {
			this.value = v;
		}
		public Value add(Value v) {
			return new StringValue(this.value + ((StringValue) v).value);
		}
		public Value eq(Value v) {
			return new IntegerValue(this.value.equals(((StringValue) v).value) ? 1 : 0);
		}
		public String toString() {
			return value;
		}
	}
	
	static class FunctionValue extends Value {
		public IEvalInContext value;
		public List<String> params;
		public FunctionValue(List<String> params, IEvalInContext v) {
			this.value = v;
			this.params = params;
		}
		public String toString() {
			return "function" + params;
		}
	}
	
	static class InternalValue extends Value {
		public Object value;
		public InternalValue(Object v) {
			this.value = v;
		}
		public String toString() {
			return value.toString();
		}
	}
	
	public static Value eval(String s) {
		Context ctx = new Context();
		return eval(s, ctx);
	}
	
	public static Value eval(String s, Context ctx) {
		ctx.put("map", new FunctionValue(new LinkedList<String>(), new IEvalInContext() {
			public Value evalInContext(Context global, Context ctx) {
				return new InternalValue(new HashMap<String,Value>());
			}
		}));
		ctx.put("mput", new FunctionValue(Arrays.asList("m","k","v"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				String k = ctx.get("k").toString();
				Value v = ctx.get("v");
				InternalValue m = (InternalValue) ctx.get("m");
				((HashMap<String,Value>) m.value).put(k, v);
				return m;
			}
		}));
		ctx.put("mget", new FunctionValue(Arrays.asList("m","k"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				String k = ctx.get("k").toString();
				return ((HashMap<String,Value>) ((InternalValue) ctx.get("m")).value).get(k);
			}
		}));
		
		ctx.put("list", new FunctionValue(new LinkedList<String>(), new IEvalInContext() {
			public Value evalInContext(Context global, Context ctx) {
				return new InternalValue(new LinkedList<Value>());
			}
		}));
		ctx.put("lget", new FunctionValue(Arrays.asList("l","i"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				int i = ((IntegerValue)ctx.get("i")).value;
				return ((LinkedList<Value>) ((InternalValue) ctx.get("l")).value).get(i);
			}
		}));
		ctx.put("lsize", new FunctionValue(Arrays.asList("l"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				return new IntegerValue(((LinkedList<Value>) ((InternalValue) ctx.get("l")).value).size());
			}
		}));
		ctx.put("ladd", new FunctionValue(Arrays.asList("l","v"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				Value v = ctx.get("v");
				InternalValue l = (InternalValue) ctx.get("l");
				((LinkedList<Value>) l.value).add(v);
				return l;
			}
		}));
		
		ctx.put("puts", new FunctionValue(Arrays.asList("s"), new IEvalInContext() {
			public Value evalInContext(Context global, Context ctx) {
				String s = ctx.get("s").toString();
				System.out.println(s);
				return new IntegerValue(0);
			}
		}));
		ctx.put("gets", new FunctionValue(new LinkedList<>(), new IEvalInContext() {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			public Value evalInContext(Context global, Context ctx) {
				try {
					return new StringValue(in.readLine());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}));
		
		ctx.put("notnull", new FunctionValue(Arrays.asList("v"), new IEvalInContext() {
			public Value evalInContext(Context global, Context ctx) {
				StringValue sv = (StringValue) ctx.get("v");
				return new IntegerValue(sv.value == null ? 0 : 1);
			}
		}));
		ctx.put("isnull", new FunctionValue(Arrays.asList("v"), new IEvalInContext() {
			public Value evalInContext(Context global, Context ctx) {
				StringValue sv = (StringValue) ctx.get("v");
				return new IntegerValue(sv.value == null ? 1 : 0);
			}
		}));
		
		return Block.parse(new Tokenizer(s)).eval(new Context(), ctx);
	}
	
	public static void main(String[] args) throws IOException {
		InputStream in = args.length == 0 ? System.in : new FileInputStream(args[0]);
		
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(line);
			}
		}
		
		eval(sb.toString());
	}
}
