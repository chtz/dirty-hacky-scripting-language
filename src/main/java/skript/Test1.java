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
			return new Value(this.value < v.value ? 1 : 0);
		}
		public String toString() {
			return "" + value;
		}
	}
	
	////
	
	public static void main(String[] args) {
		eval("( ( 0 - 2 ) * ( 3 + 4 ) < 0 - 20 / 2 ) & x", "x", 1);
	}
	
//	private static Value eval(String s) {
//		return eval(s, null);
//	}
	
	private static Value eval(String s, String name, int value) {
		Map<String, Value> vars = new HashMap<String, Value>();
		vars.put(name, new Value(value));
		return eval(s, vars);
	}
	
	private static Value eval(String s, Map<String, Value> vars) {
		System.out.println(s);
		Map<String, Value> ctx = new HashMap<String, Value>();
		if (vars != null) {
			ctx.putAll(vars);
		}
		Value result = Expression.parse(new Tokenizer(s)).eval(ctx);
		System.out.println(result);
		return result;
	}
}
