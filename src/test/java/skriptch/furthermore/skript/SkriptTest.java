package skriptch.furthermore.skript;

import static org.junit.Assert.assertEquals;
import static skriptch.furthermore.skript.Skript.eval;

import java.util.Arrays;

import org.junit.Test;

import skriptch.furthermore.skript.Skript.Context;
import skriptch.furthermore.skript.Skript.FunctionValue;
import skriptch.furthermore.skript.Skript.IEvalInContext;
import skriptch.furthermore.skript.Skript.IntegerValue;
import skriptch.furthermore.skript.Skript.Value;

public class SkriptTest {
	@Test
	public void test() {
		assertEquals("1123", eval("{ x := map ( ) ; y := map ( ) ; put ( y , 'foo' , 1000 ) ; put ( x , 'foo' , 100 ) ; put ( x , 'bar' , 23 ) ; result := get ( y , 'foo' ) + get ( x , 'foo' ) + get ( x , 'bar' ) }").toString());
		
		Context ctx = new Context();
		ctx.put("foo", new FunctionValue(Arrays.asList("a","b"), new IEvalInContext() {
			public Value evalInContext(Context global, Context ctx) {
				return new IntegerValue(((IntegerValue)ctx.get("a")).value + ((IntegerValue)ctx.get("b")).value);
			}
		}));
		assertEquals("6", eval("{ X := foo ( 1 , 2 ) + 3 }", ctx).toString());
		
		assertEquals("9", eval("{ def add ( a ,  b ) { result := a + b } ; x := 1 ; D := add ( x , 2 + 3 ) + add ( 1 , 2 ) }").toString());
		assertEquals("117", eval("{ def hello { C := C + 3 ; result := 10 } ; C := 100 ; hello ( ) ; D := 1 + hello ( ) ; D := D + C }").toString());
		
		assertEquals("ab", eval("{ C := 'a' + 'b' }").toString());
		
		ctx = new Context();
		ctx.put("x", new IntegerValue(2));
		assertEquals("5", eval("{ C := x ; for i := 0 ; i < 3 ; i := i + 1 { C := C + 1 } }", ctx).toString());
		
		ctx = new Context();
		ctx.put("x", new IntegerValue(-2));
		assertEquals("-6", eval("{ if x > 1 then { C := 2 * x } else { b := 3 * x ; C := b } }", ctx).toString());
		
		ctx = new Context();
		ctx.put("x", new IntegerValue(2));
		assertEquals("4",  eval("{ if x > 1 then { C := 2 * x } else { b := 3 * x ; C := b } }", ctx).toString());
	}
}
