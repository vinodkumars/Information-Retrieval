package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VBIndex implements BaseIndex {

	private static int bufferSize = 512;

	@Override
	public PostingList readPosting(FileChannel fc) {
		ByteBuffer buf = ByteBuffer.allocate(1);
		List<Byte> encodedBytes = new ArrayList<Byte>();
		PostingList p = null;
		List<Integer> vbDecodedPosts = new ArrayList<Integer>();
		try {
			// Get TermId
			boolean isCBit1 = false;
			while (!isCBit1) {
				buf.clear();
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				byte b = buf.get();
				encodedBytes.add(b);
				isCBit1 = ((b & 0x80) == 128);
			}
			int decodedVal = VBDecode(encodedBytes);
			p = new PostingList(decodedVal);

			// Get posting list length in bytes
			isCBit1 = false;
			encodedBytes.clear();
			while (!isCBit1) {
				buf.clear();
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				byte b = buf.get();
				encodedBytes.add(b);
				isCBit1 = ((b & 0x80) == 128);
			}
			int listByteCount = VBDecode(encodedBytes);

			// Read all bytes from disk
			int bufCapacity = listByteCount >= bufferSize ? bufferSize
					: listByteCount;
			buf = ByteBuffer.allocate(bufCapacity);
			encodedBytes.clear();
			while (listByteCount >= bufferSize) {
				buf.clear();
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				while (buf.hasRemaining()) {
					encodedBytes.add(buf.get());
				}
				listByteCount -= bufferSize;
			}
			buf = ByteBuffer.allocate(listByteCount);
			buf.clear();
			if (fc.read(buf) < 0) {
				return null;
			}
			buf.rewind();
			while (buf.hasRemaining()) {
				encodedBytes.add(buf.get());
			}

			// VB Decode the encoded bytes
			Iterator<Byte> itr = encodedBytes.iterator();
			List<Byte> bytesToDecode = new ArrayList<Byte>();
			while (itr.hasNext()) {
				byte b = itr.next().byteValue();
				bytesToDecode.add(b);
				if ((b & 0x80) == 128) {
					decodedVal = VBDecode(bytesToDecode);
					vbDecodedPosts.add(decodedVal);
					bytesToDecode.clear();
				}
			}
			// Gap decode and all the doc Ids to the posting list
			GapDecode(vbDecodedPosts);
			p.getList().addAll(vbDecodedPosts);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return p;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		ByteBuffer buf = ByteBuffer.allocate(bufferSize);
		List<Byte> encodedPosts = new ArrayList<Byte>();
		List<Integer> posts = new ArrayList<Integer>();
		posts.addAll(p.getList());

		try {
			GapEncode(posts);
			Iterator<Integer> itrPost = posts.iterator();
			while (itrPost.hasNext()) {
				encodedPosts.addAll(VBEncode(itrPost.next().intValue()));
			}

			encodedPosts.addAll(0, VBEncode(encodedPosts.size()));
			encodedPosts.addAll(0, VBEncode(p.getTermId()));
			Iterator<Byte> itrByte = encodedPosts.iterator();
			while (itrByte.hasNext()) {
				PutByteInBuf(buf, fc, itrByte.next().byteValue());
			}
			FlushBuffer(buf, fc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
