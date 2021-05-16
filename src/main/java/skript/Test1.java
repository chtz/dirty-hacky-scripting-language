package skript;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Test1 {
	static class Context { //FIXME use
		public Map<String, Value> vars = new HashMap<String, Value>();
		public void set(String name, Value value) {
			vars.put(name, value);
		}
		public Value get(String name) {
			return vars.get(name);
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
		
		public Value eval(Map<String,Value> ctx) {
			for (Statement s : statements) {
				s.eval(ctx);
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
			else {
				return AssignmentStatement.parse(t);
			}
		}
		
		public abstract void eval(Map<String, Value> ctx);
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
		
		public void eval(Map<String, Value> ctx) {
			ctx.put(variable, expression.eval(ctx));
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

		public void eval(Map<String, Value> ctx) {
			if (expression.eval(ctx).value == 1) {
				thenBlock.eval(ctx);
			}
			else if (elseBlock != null) {
				elseBlock.eval(ctx);
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
		
		public Value eval(Map<String,Value> ctx) {
			RelationalOperatorSimpleExpression last = null;
			Value result = null;
			for (RelationalOperatorSimpleExpression rose : relationalOperatorSimpleExpressions) {
				if (last == null) {
					if (rose.relationalOperator != null) throw new RuntimeException("Unknown relationalOperator=" + rose.relationalOperator);
					result = rose.simpleExpression.eval(ctx);
				}
				else {
					if ("=".equals(rose.relationalOperator)) {
						result = result.eq(rose.simpleExpression.eval(ctx));
					}
					else if (">".equals(rose.relationalOperator)) {
						result = result.gt(rose.simpleExpression.eval(ctx));
					}
					else if ("<".equals(rose.relationalOperator)) {
						result = result.lt(rose.simpleExpression.eval(ctx));
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
		
		public Value eval(Map<String,Value> ctx) {
			AdditionOperatorTerm last = null;
			Value result = null;
			for (AdditionOperatorTerm aot : additionOperatorTerms) {
				if (last == null) {
					if (aot.additionOperator != null) throw new RuntimeException("Unexpected additionOperator=" + aot.additionOperator);
					result = aot.term.eval(ctx);
				}
				else {
					if ("+".equals(aot.additionOperator)) {
						result = result.add(aot.term.eval(ctx));
					}
					else if ("-".equals(aot.additionOperator)) {
						result = result.sub(aot.term.eval(ctx));
					}
					else if ("OR".equals(aot.additionOperator)) {
						result = result.or(aot.term.eval(ctx));
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
		
		public Value eval(Map<String,Value> ctx) {
			MultiplicationOperatorFactor last = null;
			Value result = null;
			for (MultiplicationOperatorFactor mof : multiplicationOperatorFactors) {
				if (last == null) {
					if (mof.multiplicationOperator != null) throw new RuntimeException("Unexpected multiplicationOperator=" + mof.multiplicationOperator);
					result = mof.factor.eval(ctx);
				}
				else {
					if ("*".equals(mof.multiplicationOperator)) {
						result = result.mul(mof.factor.eval(ctx));
					}
					else if ("/".equals(mof.multiplicationOperator)) {
						result = result.div(mof.factor.eval(ctx));
					}
					else if ("&".equals(mof.multiplicationOperator)) {
						result = result.and(mof.factor.eval(ctx));
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
		
		public Value eval(Map<String,Value> ctx) {
			if (number != null) {
				return number.eval(ctx);
			}
			else if (expression != null) {
				return expression.eval(ctx);
			}
			else if (variable != null) {
				return variable.eval(ctx);
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
		
		public Value eval(Map<String,Value> ctx) {
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
		
		public Value eval(Map<String,Value> ctx) {
			return ctx.get(name);
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
	
	public static void main(String[] args) {
//		eval("{ a := ( 0 - 2 ) * ( 3 + 4 ) ; y := ( a < 0 - 20 / 2 ) & x ; z := y + x ; }", "x", 1);
		eval("{ if x > 1 then { a := 2 * x } else { b := 3 * x } }", "x", -2);
	}
	
	private static void eval(String s, String name, int value) {
		Map<String, Value> vars = new HashMap<String, Value>();
		vars.put(name, new Value(value));
		eval(s, vars);
	}
	
	private static void eval(String s, Map<String, Value> vars) {
		System.out.println(s);
		Map<String, Value> ctx = new HashMap<String, Value>();
		if (vars != null) {
			ctx.putAll(vars);
		}
		Block.parse(new Tokenizer(s)).eval(ctx);
		System.out.println(ctx);
	}
}
