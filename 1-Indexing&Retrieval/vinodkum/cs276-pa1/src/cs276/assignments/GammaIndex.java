package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class GammaIndex implements BaseIndex {

	private static int bufferSize = 512;
	private static int bitsetIndex = 0;

	@Override
	public PostingList readPosting(FileChannel fc) {
		PostingList p = null;
		try {
			ByteBuffer buf = ByteBuffer.allocate(1);
			boolean isCBit1 = false;
			List<Byte> vbEncodedLength = new ArrayList<Byte>();
			while (!isCBit1) {
				buf.clear();
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				byte b = buf.get();
				vbEncodedLength.add(b);
				isCBit1 = ((b & 0x80) == 128);
			}
			int bytesToReadCount = VBDecode(vbEncodedLength);
			byte[] bytesRead = new byte[bytesToReadCount];
			buf.clear();

			int bufferSizeToAllocate = bytesToReadCount >= bufferSize ? bufferSize
					: bytesToReadCount;
			buf = ByteBuffer.allocate(bufferSizeToAllocate);
			int i = 0;
			buf.clear();
			while (bytesToReadCount >= bufferSizeToAllocate) {
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				while (buf.hasRemaining()) {
					bytesRead[i] = buf.get();
					i++;
				}
				buf.clear();
				bytesToReadCount -= bufferSizeToAllocate;
			}
			if (bytesToReadCount != 0) {
				buf = ByteBuffer.allocate(bytesToReadCount);
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				while (buf.hasRemaining()) {
					bytesRead[i] = buf.get();
					i++;
				}
				buf.clear();
			}

			BitSet bs = ConvertByteArrayToBitSet(bytesRead);
			bitsetIndex = 0;
			int bitCount = UnaryDecode(bs, bytesRead.length * 8);
			if (bitCount == -1) {
				return null;
			}
			int termId = GammaDecode(bs, bitCount);
			p = new PostingList(termId);
			List<Integer> gammaDecodedPosts = new ArrayList<Integer>();
			while (true) {
				bitCount = UnaryDecode(bs, bytesRead.length * 8);
				if (bitCount == -1) {
					break;
				}
				int docId = GammaDecode(bs, bitCount);
				gammaDecodedPosts.add(docId);
			}
			GapDecode(gammaDecodedPosts);
			p.getList().addAll(gammaDecodedPosts);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return p;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		try {
			bitsetIndex = 0;
			BitSet bs = new BitSet();
			ByteBuffer buf = ByteBuffer.allocate(bufferSize);
			List<Integer> gapEncodedPosts = new ArrayList<Integer>();
			gapEncodedPosts.addAll(p.getList());

			GapEncode(gapEncodedPosts);
			GammaEncode(bs, p.getTermId());
			for (Integer d : gapEncodedPosts) {
				GammaEncode(bs, d.intValue());
			}
			FillRestTo1(bs);

			byte[] gammaEncodedBytes = bs.toByteArray();
			List<Byte> vbEncodedLength = VBEncode(gammaEncodedBytes.length);
			for (Byte b : vbEncodedLength) {
				PutByteInBuf(buf, fc, b.byteValue());
			}
			for (byte b : gammaEncodedBytes) {
				PutByteInBuf(buf, fc, b);
			}
			FlushBuffer(buf, fc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void GammaEncode(BitSet bs, int n) {
		if (n == 0) {
			return;
		}
		if (n == 1) {
			bs.set(bitsetIndex, false);
			bitsetIndex++;
			return;
		}
		int bitCount = GetBitCountToRepresentNumber(n);
		UnaryEncode(bs, bitCount - 1);
		for (int i = bitCount - 1; i > 0; i--) {
			if ((n & 0x1) == 1) {
				bs.set(bitsetIndex + i - 1, true);
			} else {
				bs.set(bitsetIndex + i - 1, false);
			}
			n = n >> 1;
		}
		bitsetIndex += (bitCount - 1);
	}

	public static int GammaDecode(BitSet bs, int bitCount) {
		int retVal = 1;
		if (bitCount == 0) {
			return 1;
		}
		for (int i = 0; i < bitCount; i++) {
			retVal = retVal << 1;
			if (bs.get(bitsetIndex)) {
				retVal = retVal | 0x1;
			}
			bitsetIndex++;
		}
		return retVal;
	}

	public static void UnaryEncode(BitSet bs, int n) {
		if (n == 0) {
			return;
		}
		while (n != 0) {
			bs.set(bitsetIndex);
			bitsetIndex++;
			n--;
		}
		bs.set(bitsetIndex, false);
		bitsetIndex++;
	}

	public static int UnaryDecode(BitSet bs, int totalBits) {
		int retVal = 0;
		if (bitsetIndex >= totalBits) {
			return -1;
		}
		while (bs.get(bitsetIndex)) {
			retVal++;
			bitsetIndex++;
			if (bitsetIndex >= totalBits) {
				return -1;
			}
		}
		bitsetIndex++;
		return retVal;
	}

	public static void FillRestTo1(BitSet bs) {
		if ((bitsetIndex % 8) == 0) {
			for (int i = 0; i < 8; i++) {
				bs.set(bitsetIndex);
				bitsetIndex++;
			}
			return;
		}
		while ((bitsetIndex % 8) != 0) {
			bs.set(bitsetIndex, true);
			bitsetIndex++;
		}
	}

	public static int GetBitCountToRepresentNumber(int n) {
		int retVal = 0;
		while (n != 0) {
			retVal++;
			n = n >> 1;
		}
		return retVal;
	}

	public static BitSet ConvertByteArrayToBitSet(byte[] arr) {
		int i = 0;
		if (arr == null) {
			return null;
		}
		BitSet bs = new BitSet();
		for (int j = 0; j < arr.length; j++) {
			byte b = arr[j];
			if (b == 0) {
				continue;
			}
			i = j * 8;
			while (b != 0) {
				if ((b & 0x1) == 1) {
					bs.set(i);
				}
				b = (byte) (b >>> 1);
				if (b < 0) {
					b = (byte) (b & 0x7F);
				}
				i++;
			}
		}
		return bs;
	}

	public static void FlushBuffer(ByteBuffer buf, FileChannel fc)
			throws IOException {
		if (buf.position() != 0) {
			buf.flip();
			while (buf.hasRemaining()) {
				fc.write(buf);
			}
			buf.clear();
		}
	}

	public static void PutByteInBuf(ByteBuffer buf, FileChannel fc, byte b)
			throws IOException {
		if (!buf.hasRemaining()) {
			buf.flip();
			while (buf.hasRemaining()) {
				fc.write(buf);
			}
			buf.clear();
		}
		buf.put(b);
	}

	public static void GapEncode(List<Integer> l) {
		for (int i = l.size() - 1; i > 0; i--) {
			l.set(i, l.get(i).intValue() - l.get(i - 1).intValue());
		}
	}

	public static void GapDecode(List<Integer> l) {
		for (int i = 1; i < l.size(); i++) {
			l.set(i, l.get(i).intValue() + l.get(i - 1).intValue());
		}
	}

	public static void PutIntInBuf(ByteBuffer buf, FileChannel fc, int n)
			throws IOException {
		if (!buf.hasRemaining()) {
			buf.flip();
			while (buf.hasRemaining()) {
				fc.write(buf);
			}
			buf.clear();
		}
		buf.putInt(n);
	}

	public static List<Byte> VBEncode(int n) {
		List<Byte> retVal = new ArrayList<Byte>();
		if (n == 0) {
			retVal.add((byte) 0x80);
			return retVal;
		}
		while (n != 0) {
			retVal.add(0, (byte) (n & 0x7F));
			if (retVal.size() == 1) {
				retVal.set(0, (byte) (retVal.get(0) | 0x80));
			}
			n = n >>> 7;
		}
		return retVal;
	}

	public static int VBDecode(List<Byte> bl) {
		int retVal = 0;
		Iterator<Byte> itr = bl.iterator();
		while (itr.hasNext()) {
			retVal = retVal << 7;
			retVal |= (itr.next().byteValue() & 0x7F);
		}
		return retVal;
	}

}
