package skinsrestorer.libs.org.json.simple.parser;

class Yylex {

	public static final int YYEOF = -1;

	private static final int ZZ_BUFFERSIZE = 16384;

	public static final int YYINITIAL = 0;
	public static final int STRING_BEGIN = 2;

	private static final int ZZ_LEXSTATE[] = { 0, 0, 1, 1 };

	private static final String ZZ_CMAP_PACKED = "\11\0\1\7\1\7\2\0\1\7\22\0\1\7\1\0\1\11\10\0" + "\1\6\1\31\1\2\1\4\1\12\12\3\1\32\6\0\4\1\1\5" + "\1\1\24\0\1\27\1\10\1\30\3\0\1\22\1\13\2\1\1\21" + "\1\14\5\0\1\23\1\0\1\15\3\0\1\16\1\24\1\17\1\20" + "\5\0\1\25\1\0\1\26\uff82\0";

	private static final char[] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

	private static final int[] ZZ_ACTION = zzUnpackAction();

	private static final String ZZ_ACTION_PACKED_0 = "\2\0\2\1\1\2\1\3\1\4\3\1\1\5\1\6" + "\1\7\1\10\1\11\1\12\1\13\1\14\1\15\5\0" + "\1\14\1\16\1\17\1\20\1\21\1\22\1\23\1\24" + "\1\0\1\25\1\0\1\25\4\0\1\26\1\27\2\0" + "\1\30";

	private static int[] zzUnpackAction() {
		int[] result = new int[45];
		int offset = 0;
		offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackAction(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			do {
				result[j++] = value;
			} while (--count > 0);
		}
		return j;
	}

	private static final int[] ZZ_ROWMAP = zzUnpackRowMap();

	private static final String ZZ_ROWMAP_PACKED_0 = "\0\0\0\33\0\66\0\121\0\154\0\207\0\66\0\242" + "\0\275\0\330\0\66\0\66\0\66\0\66\0\66\0\66" + "\0\363\0\u010e\0\66\0\u0129\0\u0144\0\u015f\0\u017a\0\u0195" + "\0\66\0\66\0\66\0\66\0\66\0\66\0\66\0\66" + "\0\u01b0\0\u01cb\0\u01e6\0\u01e6\0\u0201\0\u021c\0\u0237\0\u0252" + "\0\66\0\66\0\u026d\0\u0288\0\66";

	private static int[] zzUnpackRowMap() {
		int[] result = new int[45];
		int offset = 0;
		offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackRowMap(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int high = packed.charAt(i++) << 16;
			result[j++] = high | packed.charAt(i++);
		}
		return j;
	}

	private static final int ZZ_TRANS[] = { 2, 2, 3, 4, 2, 2, 2, 5, 2, 6, 2, 2, 7, 8, 2, 9, 2, 2, 2, 2, 2, 10, 11, 12, 13, 14, 15, 16, 16, 16, 16, 16, 16, 16, 16, 17, 18, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, 19, 20, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, 20, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 21, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 22, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 23, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, 16, 16, 16, 16, 16, 16, 16, 16, -1, -1, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, -1, -1, -1, -1, -1, -1, -1, -1, 24, 25, 26, 27, 28, 29, 30, 31, 32, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 33, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 34, 35, -1, -1, 34, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, 36, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 37, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 39, -1, 39, -1, 39, -1, -1, -1, -1, -1, 39, 39, -1, -1, -1, -1, 39, 39, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 33, -1, 20, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 20, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, 35, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 40, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 41, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 42, -1, 42, -1, 42, -1, -1, -1, -1, -1, 42, 42,
		-1, -1, -1, -1, 42, 42, -1, -1, -1, -1, -1, -1, -1, -1, -1, 43, -1, 43, -1, 43, -1, -1, -1, -1, -1, 43, 43, -1, -1, -1, -1, 43, 43, -1, -1, -1, -1, -1, -1, -1, -1, -1, 44, -1, 44, -1, 44, -1, -1, -1, -1, -1, 44, 44, -1, -1, -1, -1, 44, 44, -1, -1, -1, -1, -1, -1, -1, -1, };

	private static final int ZZ_UNKNOWN_ERROR = 0;
	private static final int ZZ_NO_MATCH = 1;
	private static final int ZZ_PUSHBACK_2BIG = 2;

	private static final String ZZ_ERROR_MSG[] = { "Unkown internal scanner error", "Error: could not match input", "Error: pushback value was too large" };

	private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();

	private static final String ZZ_ATTRIBUTE_PACKED_0 = "\2\0\1\11\3\1\1\11\3\1\6\11\2\1\1\11" + "\5\0\10\11\1\0\1\1\1\0\1\1\4\0\2\11" + "\2\0\1\11";

	private static int[] zzUnpackAttribute() {
		int[] result = new int[45];
		int offset = 0;
		offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackAttribute(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			do {
				result[j++] = value;
			} while (--count > 0);
		}
		return j;
	}

	private java.io.Reader zzReader;

	private int zzState;

	private int zzLexicalState = YYINITIAL;

	private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

	private int zzMarkedPos;

	private int zzCurrentPos;

	private int zzStartRead;

	private int zzEndRead;

	private int yychar;

	private boolean zzAtEOF;

	private StringBuffer sb = new StringBuffer();

	int getPosition() {
		return yychar;
	}

	Yylex(java.io.Reader in) {
		zzReader = in;
	}

	Yylex(java.io.InputStream in) {
		this(new java.io.InputStreamReader(in));
	}

	private static char[] zzUnpackCMap(String packed) {
		char[] map = new char[0x10000];
		int i = 0;
		int j = 0;
		while (i < 90) {
			int count = packed.charAt(i++);
			char value = packed.charAt(i++);
			do {
				map[j++] = value;
			} while (--count > 0);
		}
		return map;
	}

	private boolean zzRefill() throws java.io.IOException {

		if (zzStartRead > 0) {
			System.arraycopy(zzBuffer, zzStartRead, zzBuffer, 0, zzEndRead - zzStartRead);

			zzEndRead -= zzStartRead;
			zzCurrentPos -= zzStartRead;
			zzMarkedPos -= zzStartRead;
			zzStartRead = 0;
		}

		if (zzCurrentPos >= zzBuffer.length) {
			char newBuffer[] = new char[zzCurrentPos * 2];
			System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
			zzBuffer = newBuffer;
		}

		int numRead = zzReader.read(zzBuffer, zzEndRead, zzBuffer.length - zzEndRead);

		if (numRead > 0) {
			zzEndRead += numRead;
			return false;
		}
		if (numRead == 0) {
			int c = zzReader.read();
			if (c == -1) {
				return true;
			} else {
				zzBuffer[zzEndRead++] = (char) c;
				return false;
			}
		}

		return true;
	}

	/**
	 * Closes the input stream.
	 */
	public final void yyclose() throws java.io.IOException {
		zzAtEOF = true;
		zzEndRead = zzStartRead;

		if (zzReader != null) {
			zzReader.close();
		}
	}

	public final void yyreset(java.io.Reader reader) {
		zzReader = reader;
		zzAtEOF = false;
		zzEndRead = zzStartRead = 0;
		zzCurrentPos = zzMarkedPos = 0;
		yychar = 0;
		zzLexicalState = YYINITIAL;
	}

	public final int yystate() {
		return zzLexicalState;
	}

	public final void yybegin(int newState) {
		zzLexicalState = newState;
	}

	public final String yytext() {
		return new String(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
	}

	public final char yycharat(int pos) {
		return zzBuffer[zzStartRead + pos];
	}

	public final int yylength() {
		return zzMarkedPos - zzStartRead;
	}

	private void zzScanError(int errorCode) {
		String message;
		try {
			message = ZZ_ERROR_MSG[errorCode];
		} catch (ArrayIndexOutOfBoundsException e) {
			message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
		}

		throw new Error(message);
	}

	public void yypushback(int number) {
		if (number > yylength()) {
			zzScanError(ZZ_PUSHBACK_2BIG);
		}

		zzMarkedPos -= number;
	}

	public Yytoken yylex() throws java.io.IOException, ParseException {
		int zzInput;
		int zzAction;

		int zzCurrentPosL;
		int zzMarkedPosL;
		int zzEndReadL = zzEndRead;
		char[] zzBufferL = zzBuffer;
		char[] zzCMapL = ZZ_CMAP;

		int[] zzTransL = ZZ_TRANS;
		int[] zzRowMapL = ZZ_ROWMAP;
		int[] zzAttrL = ZZ_ATTRIBUTE;

		while (true) {
			zzMarkedPosL = zzMarkedPos;

			yychar += zzMarkedPosL - zzStartRead;

			zzAction = -1;

			zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

			zzState = ZZ_LEXSTATE[zzLexicalState];

			zzForAction: {
				while (true) {

					if (zzCurrentPosL < zzEndReadL) {
						zzInput = zzBufferL[zzCurrentPosL++];
					} else if (zzAtEOF) {
						zzInput = YYEOF;
						break zzForAction;
					} else {
						zzCurrentPos = zzCurrentPosL;
						zzMarkedPos = zzMarkedPosL;
						boolean eof = zzRefill();
						zzCurrentPosL = zzCurrentPos;
						zzMarkedPosL = zzMarkedPos;
						zzBufferL = zzBuffer;
						zzEndReadL = zzEndRead;
						if (eof) {
							zzInput = YYEOF;
							break zzForAction;
						} else {
							zzInput = zzBufferL[zzCurrentPosL++];
						}
					}
					int zzNext = zzTransL[zzRowMapL[zzState] + zzCMapL[zzInput]];
					if (zzNext == -1) {
						break zzForAction;
					}
					zzState = zzNext;

					int zzAttributes = zzAttrL[zzState];
					if ((zzAttributes & 1) == 1) {
						zzAction = zzState;
						zzMarkedPosL = zzCurrentPosL;
						if ((zzAttributes & 8) == 8) {
							break zzForAction;
						}
					}

				}
			}

			zzMarkedPos = zzMarkedPosL;

			switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
				case 11: {
					sb.append(yytext());
				}
				case 25:
					break;
				case 4: {
					sb.delete(0, sb.length());
					yybegin(STRING_BEGIN);
				}
				case 26:
					break;
				case 16: {
					sb.append('\b');
				}
				case 27:
					break;
				case 6: {
					return new Yytoken(Yytoken.TYPE_RIGHT_BRACE, null);
				}
				case 28:
					break;
				case 23: {
					Boolean val = Boolean.valueOf(yytext());
					return new Yytoken(Yytoken.TYPE_VALUE, val);
				}
				case 29:
					break;
				case 22: {
					return new Yytoken(Yytoken.TYPE_VALUE, null);
				}
				case 30:
					break;
				case 13: {
					yybegin(YYINITIAL);
					return new Yytoken(Yytoken.TYPE_VALUE, sb.toString());
				}
				case 31:
					break;
				case 12: {
					sb.append('\\');
				}
				case 32:
					break;
				case 21: {
					Double val = Double.valueOf(yytext());
					return new Yytoken(Yytoken.TYPE_VALUE, val);
				}
				case 33:
					break;
				case 1: {
					throw new ParseException(yychar, ParseException.ERROR_UNEXPECTED_CHAR, new Character(yycharat(0)));
				}
				case 34:
					break;
				case 8: {
					return new Yytoken(Yytoken.TYPE_RIGHT_SQUARE, null);
				}
				case 35:
					break;
				case 19: {
					sb.append('\r');
				}
				case 36:
					break;
				case 15: {
					sb.append('/');
				}
				case 37:
					break;
				case 10: {
					return new Yytoken(Yytoken.TYPE_COLON, null);
				}
				case 38:
					break;
				case 14: {
					sb.append('"');
				}
				case 39:
					break;
				case 5: {
					return new Yytoken(Yytoken.TYPE_LEFT_BRACE, null);
				}
				case 40:
					break;
				case 17: {
					sb.append('\f');
				}
				case 41:
					break;
				case 24: {
					try {
						int ch = Integer.parseInt(yytext().substring(2), 16);
						sb.append((char) ch);
					} catch (Exception e) {
						throw new ParseException(yychar, ParseException.ERROR_UNEXPECTED_EXCEPTION, e);
					}
				}
				case 42:
					break;
				case 20: {
					sb.append('\t');
				}
				case 43:
					break;
				case 7: {
					return new Yytoken(Yytoken.TYPE_LEFT_SQUARE, null);
				}
				case 44:
					break;
				case 2: {
					Long val = Long.valueOf(yytext());
					return new Yytoken(Yytoken.TYPE_VALUE, val);
				}
				case 45:
					break;
				case 18: {
					sb.append('\n');
				}
				case 46:
					break;
				case 9: {
					return new Yytoken(Yytoken.TYPE_COMMA, null);
				}
				case 47:
					break;
				case 3: {
				}
				case 48:
					break;
				default:
					if ((zzInput == YYEOF) && (zzStartRead == zzCurrentPos)) {
						zzAtEOF = true;
						return null;
					} else {
						zzScanError(ZZ_NO_MATCH);
					}
			}
		}
	}

}
