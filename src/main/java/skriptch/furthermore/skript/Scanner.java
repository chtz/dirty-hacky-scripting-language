package skriptch.furthermore.skript;

import java.util.PrimitiveIterator.OfInt;
import java.util.Stack;

public class Scanner {
	final OfInt i;
	final Stack<Integer> pushedback = new Stack<>();
	
	public Scanner(String s) {
		i = s.chars().iterator();
	}
	
	public int next() {
		if (pushedback.isEmpty()) {
			return i.hasNext() ? i.nextInt() : -1;
		}
		else {
			return pushedback.pop();
		}
	}
	
	public int peek() {
		int c = next();
		if (c != -1) {
			pushback(c);
		}
		return c;
	}
	
	private void pushback(int c) {
		if (c != -1) {
			pushedback.push(c);
		}
	}

	public String nextToken() {
		int c = peek();
		
		if (c == -1) return null;
		
		if (c == '/') {
			c = next();
			int c2 = peek();
			pushback(c2);
			pushback(c);
			
			if (c2 == '*') {
				consumeComment();
				return nextToken();
			}
		}
		
		if (c == ':') {
			c = next();
			int c2 = next();
			if (c2 == '=') {
				return ":=";
			}
			else {
				pushback(c2);
				pushback(c);
			}
		}
		
		if (isWhitespace(c)) {
			consumeWhitespace();
			return nextToken();
		}
		if (isApostroph(c)) return nextString();
		if (isDigit(c)) return nextNumberToken();
		if (isLetter(c)) return nextIdentifierToken();
		if (isOneCharSymbol(c)) return nextOneCharSymbol();
		
		throw new RuntimeException("unexpected character " + c + " / '"  + ((char) c) + "'");
	}

	private void consumeComment() {
		int c = next();
		if (c != '/') throw new RuntimeException("comment: invalid character '" + ((char) c) + "'");
		
		c = next();
		if (c != '*') throw new RuntimeException("comment: invalid character '" + ((char) c) + "'");
		
		for (;;) {
			c = next();
			
			if (c == -1) throw new RuntimeException("comment: unexpected EOF");
			
			if (c == '*') {
				c = next();
				
				if (c == -1) throw new RuntimeException("comment: unexpected EOF");
				
				if (c == '/') {
					break;
				}
			}
		}
	}

	private void consumeWhitespace() {
		for (;;) {
			int c = next();
			
			if (c == -1) break;
			
			if (!isWhitespace(c)) {
				pushback(c);
				break;
			}
		}
	}

	private String nextString() {
		StringBuilder sb = new StringBuilder();
		
		int c = next();
		if (!isApostroph(c)) throw new RuntimeException("string: invalid character '" + ((char) c) + "'"); 
		sb.append((char) c);
		
		for (;;) {
			c = next();
			
			if (isBackslash(c)) {
				c = next();
				
				if (c == -1) throw new RuntimeException("string: invalid character '" + ((char) c) + "'");
				
				sb.append((char) c);
			}
			else {
				sb.append((char) c);
				
				if (isApostroph(c)) {
					break;
				}
			}
		}
		
		return sb.toString();
	}

	private boolean isBackslash(int c) {
		return c == '\\';
	}

	private boolean isApostroph(int c) {
		return c == '\'';
	}

	private boolean isOneCharSymbol(int c) {
		return c == '{'
				|| c == '}'
				|| c == '('
				|| c == ')'
				|| c == '+'
				|| c == '-'
				|| c == '*'
				|| c == '/'
				|| c == '|'
				|| c == '&'
				|| c == '='
				|| c == '>'
				|| c == '<'
				|| c == ':'
				|| c == ','
				|| c == ';';
	}

	private boolean isLetter(int c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	private boolean isWhitespace(int c) {
		return c == -1 || c == ' ' || c == '\t' || c == '\r' || c == '\n';
	}
	
	private boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}
	
	private String nextOneCharSymbol() {
		int c = next();
		if (!isOneCharSymbol(c)) throw new RuntimeException("one-char-symbol: invalid character '" + ((char) c) + "'");
		return new String(""+(char) c);
	}

	private String nextIdentifierToken() { 
		StringBuilder sb = new StringBuilder();
		
		int c = next();
		if (isLetter(c)) {
			sb.append((char) c);
		}
		else throw new RuntimeException("identifier: invalid character '" + ((char) c) + "'");
		
		for (;;) {
			c = next();
			if (isDigit(c) || isLetter(c)) {
				sb.append((char) c);
			}
			else if (isWhitespace(c) || isOneCharSymbol(c)) {
				pushback(c);
				break;
			}
			else throw new RuntimeException("identifier: invalid character '" + ((char) c) + "'");
		}
		return sb.toString();
	}
	
	private String nextNumberToken() {
		StringBuilder sb = new StringBuilder();
		for (;;) {
			int c = next();
			if (isDigit(c)) {
				sb.append((char) c);
			}
			else if (isWhitespace(c) || isOneCharSymbol(c)) {
				pushback(c);
				break;
			}
			else throw new RuntimeException("number: invalid character '" + ((char) c) + "'");
		}
		return sb.toString();
	}
}
