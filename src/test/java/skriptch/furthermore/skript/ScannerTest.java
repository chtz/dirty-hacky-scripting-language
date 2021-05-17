package skriptch.furthermore.skript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ScannerTest {
	@Test
	public void test() {
		assertEquals("1234567890", new Scanner("1234567890").nextToken());
		assertEquals("1234567890", new Scanner("1234567890 ").nextToken());
		
		assertEquals("abc", new Scanner("abc").nextToken());
		assertEquals("abc123", new Scanner("abc123").nextToken());
		assertEquals("abc123", new Scanner("abc123\n").nextToken());
		
		assertEquals("+", new Scanner("+").nextToken());
		assertEquals("+", new Scanner("+-").nextToken());
		
		assertEquals("'hello'", new Scanner("'hello'").nextToken());
		assertEquals("'hello'", new Scanner("'hello'\r").nextToken());
		assertEquals("'hel'l\\o'", new Scanner("'hel\\'l\\\\o'\t").nextToken());
		
		Scanner s = new Scanner("hallo \t\n\r 123 { /* 'hallo' / * / ' */ 'halli \" hallo' ");
		assertEquals("hallo", s.nextToken());
		assertEquals("123", s.nextToken());
		assertEquals("{", s.nextToken());
		assertEquals("'halli \" hallo'", s.nextToken());
		assertNull(s.nextToken());
		
		s = new Scanner("puts('hello world');");
		assertEquals("puts", s.nextToken());
		assertEquals("(", s.nextToken());
		assertEquals("'hello world'", s.nextToken());
		assertEquals(")", s.nextToken());
		assertEquals(";", s.nextToken());
		assertNull(s.nextToken());
	}
	
	@Test(expected = RuntimeException.class)
	public void test2() {
		new Scanner("1234567890x").nextToken();
		new Scanner("ab1234567890!").nextToken();
		new Scanner("'hello").nextToken();
		new Scanner("'hello\\").nextToken();
		new Scanner("/* hallo").nextToken();
		new Scanner("/* hallo *").nextToken();
	}
}
