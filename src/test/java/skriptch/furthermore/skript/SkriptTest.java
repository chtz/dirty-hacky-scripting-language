package skriptch.furthermore.skript;

import static org.junit.Assert.assertEquals;
import static skriptch.furthermore.skript.Skript.eval;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Arrays;

import org.junit.Test;

import skriptch.furthermore.skript.Skript.Context;
import skriptch.furthermore.skript.Skript.FunctionValue;
import skriptch.furthermore.skript.Skript.IEvalInContext;
import skriptch.furthermore.skript.Skript.IntegerValue;
import skriptch.furthermore.skript.Skript.InternalValue;
import skriptch.furthermore.skript.Skript.Value;

public class SkriptTest {
	@Test
	public void test() {
		assertEquals("1123", eval("{ x := map ( ) ; y := map ( ) ; mput ( y , 'foo' , 1000 ) ; mput ( x , 'foo' , 100 ) ; mput ( x , 'bar' , 23 ) ; result := mget ( y , 'foo' ) + mget ( x , 'foo' ) + mget ( x , 'bar' ) }").toString());
		assertEquals("1123", eval("{x:=map(); y:=map(); mput(y,'foo',1000); mput(x,'foo',100); mput(x,'bar',23); result:=mget(y,'foo')+mget(x,'foo')+mget(x,'bar')}").toString());
		
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
		
		assertEquals("5",  eval("{ l := list(); ladd(l,1); ladd(l,2); result := lget(l,0)+lget(l,1)+lsize(l) }", ctx).toString());
	}
	
	@Test
	public void samples() throws FileNotFoundException, IOException {
		try (PrintStream out = new PrintStream(new FileOutputStream("samples.md"))) {
			for (File f : new File("samples").listFiles()) {
				if (f.getName().endsWith(".sk")) {
					String script;
					try (InputStream in = new FileInputStream(f)) {
						script = Skript.readLines(in);
					}
					
					File fin = new File("samples/" + f.getName() + ".txt");
					String input = "";
					if (fin.exists()) {
						try (InputStream in = new FileInputStream(fin)) {
							input = Skript.readLines(in);
						}
					}
					
					Context ctx = new Context();
					ctx.put("in",new InternalValue(new BufferedReader(new StringReader(input))));
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					ctx.put("out", new InternalValue(new PrintStream(bout)));
					eval(script, ctx);
					
					out.println("# " + f.getName());
					out.println("Script:");
					out.println(markdownScript(script));
					if (!"".equals(input)) {
						out.println("Input:");
						out.println(markdownScript(input));
					}
					out.println("Output:");
					out.println(markdownScript(new String(bout.toByteArray())));
				}
			}
		}
	}

	private String markdownScript(String s) {
		return "```\n" + s + (s.endsWith("\n") ? "" : "\n") + "```";
	}
}
