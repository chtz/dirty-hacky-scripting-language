package skript;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;

public class Test1 {
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
		private String pushedBack;
		public Tokenizer(String s) {
			st = new StringTokenizer(s);
		}
		public boolean hasMore() {
			return pushedBack != null || st.hasMoreTokens();
		}
		public String next() {
			if (pushedBack != null) {
				try {
					return pushedBack;
				}
				finally {
					pushedBack = null;
				}
			}
			else {
				return st.hasMoreTokens() ? st.nextToken() : null;
			}
		}
		
		public void pushback(String token) {
			pushedBack = token;
		}
	}
	
	static class Block {
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
			for (Statement s : statements) {
				s.eval(global, blockCtx);
			}
			return new Value(0);
		}
	}
	
	static abstract class Statement {
		public static Statement parse(Tokenizer t) {
			String token = t.next();
			t.pushback(token);
			
			if ("if".equals(token)) {
				return IfStatement.parse(t);
			}
			if ("for".equals(token)) {
				return ForStatement.parse(t);
			}
			else {
				return AssignmentStatement.parse(t);
			}
		}
		
		public abstract void eval(Context global, Context ctx);
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
		
		public void eval(Context global, Context ctx) {
			if (isGlobal(variable)) {
				global.put(variable, expression.eval(global, ctx));
			}
			else {
				ctx.put(variable, expression.eval(global, ctx));
			}
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
		
		public void eval(Context global, Context ctx) {
			Context blockCtx = new Context(ctx);
			
			init.eval(global, blockCtx);
			
			while (condition.eval(global, blockCtx).value == 1) {
				whileBlock.evalInContext(global, blockCtx);
				
				increment.eval(global, blockCtx);
			}
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

		public void eval(Context global, Context ctx) {
			if (expression.eval(global, ctx).value == 1) {
				thenBlock.eval(global, ctx);
			}
			else if (elseBlock != null) {
				elseBlock.eval(global, ctx);
			}
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
		public Number number;         //XOR
		public Expression expression; //XOR
		public Variable variable;	  //XOR

		public static Factor parse(Tokenizer t) {
			Factor factor = new Factor();
			
			String token = t.next();
			
			if ("(".equals(token)) {
				factor.expression = Expression.parse(t);
				
				if (!")".equals(t.next())) throw new RuntimeException(") expected");
			}
			else {
				try {
					factor.number = Number.parse(token);
				}
				catch (NumberFormatException e) {
					factor.variable = Variable.parse(token);
				}
			}
			
			return factor;
		}
		
		public Value eval(Context global, Context ctx) {
			if (number != null) {
				return number.eval(global, ctx);
			}
			else if (expression != null) {
				return expression.eval(global, ctx);
			}
			else if (variable != null) {
				return variable.eval(global, ctx);
			}
			else throw new RuntimeException("invalid Factor");
		}
	}
	
	static class Number {
		public int value;

		public static Number parse(String token) {
			Number number = new Number();
			number.value = Integer.parseInt(token);
			
			return number;
		}
		
		public Value eval(Context global, Context ctx) {
			return new Value(value);
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
		public int value;
		public Value(int v) {
			this.value = v;
		}
		public Value add(Value v) {
			System.out.println(this.value + "+" +  v.value + "=" + (this.value + v.value)); //DEBUG
			return new Value(this.value + v.value);
		}
		public Value sub(Value v) {
			System.out.println(this.value + "-" +  v.value + "=" + (this.value - v.value)); //DEBUG
			return new Value(this.value - v.value);
		}
		public Value or(Value v) {
			System.out.println(this.value + "|" +  v.value + "=" + (this.value == 1 || v.value == 1 ? 1 : 0)); //DEBUG
			return new Value(this.value == 1 || v.value == 1 ? 1 : 0);
		}
		public Value div(Value v) {
			System.out.println(this.value + "/" +  v.value + "=" + (this.value / v.value)); //DEBUG
			return new Value(this.value / v.value);
		}
		public Value mul(Value v) {
			System.out.println(this.value + "*" +  v.value + "=" + (this.value * v.value)); //DEBUG
			return new Value(this.value * v.value);
		}
		public Value and(Value v) {
			System.out.println(this.value + "&" +  v.value + "=" + (this.value == 1 && v.value == 1 ? 1 : 0)); //DEBUG
			return new Value(this.value == 1 && v.value == 1 ? 1 : 0);
		}
		public Value eq(Value v) {
			System.out.println(this.value + "=" +  v.value + "=" + (this.value == v.value ? 1 : 0)); //DEBUG
			return new Value(this.value == v.value ? 1 : 0);
		}
		public Value lt(Value v) {
			System.out.println(this.value + "<" +  v.value + "=" + (this.value < v.value ? 1 : 0)); //DEBUG
			return new Value(this.value < v.value ? 1 : 0);
		}
		public Value gt(Value v) {
			System.out.println(this.value + ">" +  v.value + "=" + (this.value > v.value ? 1 : 0)); //DEBUG
			return new Value(this.value > v.value ? 1 : 0);
		}
		public String toString() {
			return "" + value;
		}
	}
	
	////
	
	@Test
	public void test() {
		assertEquals("5", eval("{ C := x ; for i := 0 ; i < 3 ; i := i + 1 { C := C + 1 } }", "x", 2).get("C").toString());
		
		assertEquals("-6", eval("{ if x > 1 then { C := 2 * x } else { b := 3 * x ; C := b } }", "x", -2).get("C").toString());
		assertEquals("4",  eval("{ if x > 1 then { C := 2 * x } else { b := 3 * x ; C := b } }", "x",  2).get("C").toString());
	}
	
	private Context eval(String s, String name, int value) {
		Context global = new Context();
		Context ctx = new Context();
		ctx.put(name, new Value(value));
		return eval(s, global, ctx);
	}
	
	private Context eval(String s, Context global, Context ctx) {
		System.out.println(s);
		Block.parse(new Tokenizer(s)).eval(global, ctx);
		System.out.println(global.vars);
		return global;
	}
}
