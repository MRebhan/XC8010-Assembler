package xc8010.assembler;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.System.exit;

public class Instruction {

	private static final HashMap<String, HashMap<AddressingMode, Integer>> opcodeMap;
	private static final HashMap<AddressingMode, ISerializer> serializers;
	public static final ArrayList<String> relativeMap;

	public final String id;
	public final String[] arguments;

	public static Instruction fromString(String insnLine, int cptr, HashMap<String, Integer> labels) {
		ArrayList<String> labelsSorted = new ArrayList<>();
		if (labels != null) {
			labelsSorted.addAll(labels.keySet());
			labelsSorted.sort((o1, o2) -> o2.compareTo(o1));
		}
		String t = insnLine.trim();
		String id;
		String[] arguments;

		// strip out labels
		int end = t.indexOf(':');
		t = t.substring(end + 1).trim();

		end = t.indexOf(' ');
		if (end == -1) {
			id = t;
			arguments = new String[0];
		} else {
			ArrayList<String> args = new ArrayList<>();
			id = t.substring(0, end);
			t = t.substring(end + 1).trim();
			while ((end = t.indexOf(',')) != -1) {
				args.add(t.substring(0, end).trim());
				t = t.substring(end + 1).trim();
			}
			args.add(t.trim());
			arguments = new String[args.size()];
			args.toArray(arguments);
			if (!labelsSorted.isEmpty())
				for (int i = 0; i < arguments.length; i++) {
					String arg = arguments[i];
					int ind = Integer.MAX_VALUE;
					int len = 0;
					int ct = 0;
					for (String string : labelsSorted) {
						int j;
						if ((j = arg.indexOf(string)) < ind && j != -1) {
							ind = j;
							len = string.length();
							ct = labels.get(string);
						}
					}
					if (ind != Integer.MAX_VALUE) {
						// if (relativeMap.contains(id)) {
						// arg = arg.substring(0, ind) + Assembler.toString((ct
						// - cptr) & 0xFF, false)
						// + arg.substring(ind + len);
						// } else {
						arg = arg.substring(0, ind) + Assembler.toString(ct, true) + arg.substring(ind + len);
						// }
						arguments[i] = arg;
					}
				}
		}
		return new Instruction(id.toLowerCase(), arguments);

	}

	public static Instruction fromString(String insnLine) {
		return fromString(insnLine, 0, null);
	}

	public byte[] getData(int cptr) {
		ArrayList<Byte> data = new ArrayList<>(4);
		if (i("db")) {
			for (String arg : arguments) {
				if (arg.startsWith("'") && arg.endsWith("'")) {
					byte[] bytes = Assembler.stringBytes(arg.substring(1, arg.length() - 1));
					for (byte b : bytes) {
						data.add(b);
					}
				} else {
					int i = Assembler.parseInt(arg) & 0xFF;
					data.add((byte) i);
				}
			}
		} else {
			AddressingMode am = AddressingMode.getAddressingMode(this);
			int opcode = getOpcode(id, am);
			data.add((byte) opcode);
			ISerializer s = serializers.get(am);
			if (s == null) {
				System.err.printf("Unimplemented addressing mode %s", am);
				exit(1);
			}
			s.accept(this, cptr, data);
		}
		return toBytes(data);
	}

	private boolean i(String expected) {
		return expected.equals(id);
	}

	private Instruction(String id, String[] arguments) {
		this.id = id;
		this.arguments = arguments;
	}

	private static byte[] toBytes(ArrayList<Byte> data) {
		byte[] dres = new byte[data.size()];
		int i = 0;
		for (byte b : data) {
			dres[i++] = b;
		}
		return dres;
	}

	private static void registerOpcode(String insnId, AddressingMode am, int dat) {
		HashMap<AddressingMode, Integer> lvl2 = opcodeMap.get(insnId);
		if (lvl2 == null) {
			lvl2 = new HashMap<>();
			opcodeMap.put(insnId, lvl2);
		}
		if (lvl2.containsKey(am)) {
			System.out.printf("Duplicate addressing mode %s for instruction %s%n", am, insnId);
			exit(1);
		}
		lvl2.put(am, dat);
	}

	private static int getOpcode(String insnId, AddressingMode am) {
		HashMap<AddressingMode, Integer> lvl2 = opcodeMap.get(insnId);
		if (lvl2 == null) {
			System.out.printf("Invalid opcode %s (addressing mode %s)", insnId, am);
			exit(1);
		}
		if (!lvl2.containsKey(am)) {
			System.out.printf("Invalid addressing mode %s for instruction %s%n", am, insnId);
			exit(1);
		}
		return lvl2.get(am);
	}

	static {
		relativeMap = new ArrayList<>();
		relativeMap.add("bcc");
		relativeMap.add("bcs");
		relativeMap.add("beq");
		relativeMap.add("bmi");
		relativeMap.add("bne");
		relativeMap.add("bpl");
		relativeMap.add("bvc");
		relativeMap.add("bvs");
		relativeMap.add("bra");

		opcodeMap = new HashMap<>();
		registerOpcode("brk", AddressingMode.IMPLIED, 0x00);
		registerOpcode("php", AddressingMode.IMPLIED, 0x08);
		registerOpcode("inc", AddressingMode.ACCUMULATOR, 0x1A);
		registerOpcode("jsr", AddressingMode.ABSOLUTE, 0x20);
		registerOpcode("plp", AddressingMode.IMPLIED, 0x28);
		registerOpcode("pha", AddressingMode.IMPLIED, 0x48);
		registerOpcode("jmp", AddressingMode.ABSOLUTE, 0x4C);
		registerOpcode("phy", AddressingMode.IMPLIED, 0x5A);
		registerOpcode("rts", AddressingMode.IMPLIED, 0x60);
		registerOpcode("stz", AddressingMode.ZERO_PAGE, 0x64);
		registerOpcode("pla", AddressingMode.IMPLIED, 0x68);
		registerOpcode("adc", AddressingMode.IMMEDIATE, 0x69);
		registerOpcode("stz", AddressingMode.ZERO_PAGE_INDEXED_X, 0x74);
		registerOpcode("ply", AddressingMode.IMPLIED, 0x7A);
		registerOpcode("bra", AddressingMode.RELATIVE, 0x80);
		registerOpcode("sty", AddressingMode.ZERO_PAGE, 0x84);
		registerOpcode("sta", AddressingMode.ZERO_PAGE, 0x85);
		registerOpcode("dey", AddressingMode.IMPLIED, 0x88);
		registerOpcode("sty", AddressingMode.ABSOLUTE, 0x8C);
		registerOpcode("sta", AddressingMode.ABSOLUTE, 0x8D);
		registerOpcode("sta", AddressingMode.INDIRECT_INDEXED, 0x91);
		registerOpcode("sta", AddressingMode.INDIRECT_ABSOLUTE, 0x92);
		registerOpcode("sty", AddressingMode.ZERO_PAGE_INDEXED_X, 0x94);
		registerOpcode("sta", AddressingMode.ZERO_PAGE_INDEXED_X, 0x95);
		registerOpcode("sta", AddressingMode.ABSOLUTE_INDEXED_Y, 0x99);
		registerOpcode("txs", AddressingMode.IMPLIED, 0x9A);
		registerOpcode("stz", AddressingMode.ABSOLUTE, 0x9C);
		registerOpcode("ldy", AddressingMode.IMMEDIATE, 0xA0);
		registerOpcode("ldy", AddressingMode.IMMEDIATEB, 0xA0);
		registerOpcode("ldx", AddressingMode.IMMEDIATE, 0xA2);
		registerOpcode("ldx", AddressingMode.IMMEDIATEB, 0xA2);
		registerOpcode("lda", AddressingMode.ZERO_PAGE, 0xA5);
		registerOpcode("lda", AddressingMode.IMMEDIATE, 0xA9);
		registerOpcode("lda", AddressingMode.IMMEDIATEB, 0xA9);
		registerOpcode("lda", AddressingMode.ABSOLUTE, 0xAD);
		registerOpcode("lda", AddressingMode.INDIRECT_INDEXED, 0xB1);
		registerOpcode("lda", AddressingMode.ABSOLUTE_INDEXED_Y, 0xB9);
		registerOpcode("ldx", AddressingMode.ABSOLUTE_INDEXED_Y, 0xBE);
		registerOpcode("cpy", AddressingMode.IMMEDIATE, 0xC0);
		registerOpcode("cpy", AddressingMode.IMMEDIATEB, 0xC0);
		registerOpcode("rep", AddressingMode.IMMEDIATEB, 0xC2);
		registerOpcode("iny", AddressingMode.IMPLIED, 0xC8);
		registerOpcode("cmp", AddressingMode.IMMEDIATE, 0xC9);
		registerOpcode("cmp", AddressingMode.IMMEDIATEB, 0xC9);
		registerOpcode("dex", AddressingMode.IMPLIED, 0xCA);
		registerOpcode("wai", AddressingMode.IMPLIED, 0xCB);
		registerOpcode("bne", AddressingMode.RELATIVE, 0xD0);
		registerOpcode("stp", AddressingMode.IMPLIED, 0xDB);
		registerOpcode("sep", AddressingMode.IMMEDIATEB, 0xE2);
		registerOpcode("inc", AddressingMode.ZERO_PAGE, 0xE6);
		registerOpcode("inc", AddressingMode.ABSOLUTE, 0xEE);
		registerOpcode("mmu", AddressingMode.ZERO_PAGE, 0xEF);
		registerOpcode("beq", AddressingMode.RELATIVE, 0xF0);
		registerOpcode("xce", AddressingMode.IMPLIED, 0xFB);

		serializers = new HashMap<>();
		serializers.put(AddressingMode.IMMEDIATE, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0].substring(1));
			list.add((byte) (val & 0xFF));
			list.add((byte) (val >> 8));
		});
		serializers.put(AddressingMode.IMMEDIATEB, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0].substring(1));
			list.add((byte) (val & 0xFF));
		});
		serializers.put(AddressingMode.ZERO_PAGE, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0]);
			list.add((byte) (val & 0xFF));
		});
		serializers.put(AddressingMode.ZERO_PAGE_INDEXED_X, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0]);
			list.add((byte) (val & 0xFF));
		});
		serializers.put(AddressingMode.ABSOLUTE, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0]);
			list.add((byte) (val & 0xFF));
			list.add((byte) (val >> 8));
		});
		serializers.put(AddressingMode.INDIRECT_ABSOLUTE, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0].substring(1, 6));
			list.add((byte) (val & 0xFF));
			list.add((byte) (val >> 8));
		});
		serializers.put(AddressingMode.ABSOLUTE_INDEXED_Y, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0]);
			list.add((byte) (val & 0xFF));
			list.add((byte) (val >> 8));
		});
		serializers.put(AddressingMode.RELATIVE, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0]);
			val -= cptr + list.size() + 1;
			list.add((byte) val);
		});
		serializers.put(AddressingMode.INDIRECT_INDEXED, (insn, cptr, list) -> {
			int val = Assembler.parseInt(insn.arguments[0].substring(1, 6));
			list.add((byte) (val & 0xFF));
			list.add((byte) (val >> 8));
		});
		serializers.put(AddressingMode.IMPLIED, (insn, cptr, list) -> {
		});
		serializers.put(AddressingMode.ACCUMULATOR, (insn, cptr, list) -> {
		});
	}

}