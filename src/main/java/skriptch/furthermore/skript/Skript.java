package skriptch.furthermore.skript;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Skript {
	static class Context { 
		public Context parent;
		public Map<String, Value> vars = new HashMap<String, Value>();
		public Context() {}
		public Context(Context parent) {
			this.parent = parent;
		}
		public void put(String name, Value value) {
			vars.put(name, value);
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
		private StringTokenizer st;
		private LinkedList<String> pushedBack = new LinkedList<String>();
		public Tokenizer(String s) {
			st = new StringTokenizer(s);
		}
		public boolean hasMore() {
			return pushedBack != null || st.hasMoreTokens();
		}
		public String next() {
			String next;
			if (!pushedBack.isEmpty()) {
				next = pushedBack.removeFirst();
			}
			else {
				next = st.hasMoreTokens() ? st.nextToken() : null;
			}
			return next;
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
		ctx.put("put", new FunctionValue(Arrays.asList("m","k","v"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				String k = ctx.get("k").toString();
				Value v = ctx.get("v");
				((HashMap<String,Value>) ((InternalValue) ctx.get("m")).value).put(k, v);
				return v;
			}
		}));
		ctx.put("get", new FunctionValue(Arrays.asList("m","k"), new IEvalInContext() {
			@SuppressWarnings("unchecked")
			public Value evalInContext(Context global, Context ctx) {
				String k = ctx.get("k").toString();
				return ((HashMap<String,Value>) ((InternalValue) ctx.get("m")).value).get(k);
			}
		}));
		
		return Block.parse(new Tokenizer(s)).eval(new Context(), ctx);
	}
}
