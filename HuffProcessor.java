import java.util.PriorityQueue;

/**
 * @author Merle Nye
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}
	public void compress(BitInputStream in, BitOutputStream out){

		int [] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		in.reset();
		writeCompressedBits(codings,in,out);
		out.close();
		
		}
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		while(true){
			int bitter = in.readBits(BITS_PER_WORD);
			if(bitter ==-1) {
				break;
			}
			String newS =codings[bitter];
			out.writeBits(newS.length(), Integer.parseInt(newS, 2));
		}
		String ender = codings[PSEUDO_EOF];
		out.writeBits(ender.length(), Integer.parseInt(ender,2));
		
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		if (root.myLeft != null || root.myRight != null ) {
			out.writeBits(1, 0);
			writeHeader(root.myLeft, out);
			writeHeader(root.myRight, out);
		}
		else {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);
		}
		
	}

	private int[] readForCounts(BitInputStream in) {
		int[] ans = new int[ALPH_SIZE+1];
		for (int i =0; i<ans.length;i++) {
			ans[i]=0;
		}
		while(true) {
		int store = in.readBits(BITS_PER_WORD);
		if (store == -1) {
			ans[PSEUDO_EOF] = 1;
			break;
		}
		else {
		ans[store] = ans[store] + 1;
	}
		}
		return ans;

	}

	private HuffNode makeTreeFromCounts(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int i= 0; i<counts.length; i ++) {
			if (counts[i]!= 0) {
		    pq.add(new HuffNode(i,counts[i],null,null));
		}
		}

		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    HuffNode t = new HuffNode(0,left.myWeight+right.myWeight, left, right);
		    pq.add(t);
		}
		HuffNode root = pq.remove();

		return root;
	}
	private String[] makeCodingsFromTree(HuffNode root) {
			String[] encodings = new String[ALPH_SIZE +1];
			doWork(root,"", encodings);
			return encodings;
	}
		private void doWork(HuffNode t,String hey, String[] path) {
			if (t.myLeft == null && t.myRight == null) {
				path[t.myValue] = hey;
				return;
			}
			doWork(t.myLeft,hey.concat("0"),path);
			doWork(t.myRight,hey.concat("1"), path);
		}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with " +bits);
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}

	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		 HuffNode current = root; 
		   while (true) {
		       int bits = in.readBits(1);
		       if (bits == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       }
		       else { 
		           if (bits == 0) current = current.myLeft;
		      else current = current.myRight;

		           if (current.myLeft == null && current.myRight == null) {
		               if (current.myValue == PSEUDO_EOF) 
		                   break; 
		               else {
		            	  out.writeBits(BITS_PER_WORD, current.myValue);
		                  current = root;
		               }
		           }
		       }
		   }

		
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		int onebit = in.readBits(1);
		
		if(onebit == -1) {
			throw new HuffException("No bit");
		}
		if (onebit == 0) {
			HuffNode left = readTreeHeader(in);
			HuffNode right =readTreeHeader(in);
			return new HuffNode (0,0,left,right);
		}
		else {
			int other = in.readBits(BITS_PER_WORD+1);
			System.out.println(other);
			return new HuffNode(other, 0, null, null);
		}
	} 
}