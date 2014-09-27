package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class BasicIndex implements BaseIndex {

	private static int bufferSize = 512;
	
	@Override
	public PostingList readPosting(FileChannel fc) {
		/*
		 * Your code here
		 */
		ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
		PostingList p = null;
		int listCount = 0;
		try {
			buf.clear();
			if (fc.read(buf) < 0) {
				return null;
			}
			buf.rewind();
			p = new PostingList(buf.getInt());

			buf.clear();
			if (fc.read(buf) < 0) {
				return null;
			}
			buf.rewind();
			listCount = buf.getInt();

			buf = ByteBuffer.allocate(bufferSize * (Integer.SIZE / Byte.SIZE));
			while (listCount >= bufferSize) {
				buf.clear();
				if (fc.read(buf) < 0) {
					return null;
				}
				buf.rewind();
				while (buf.hasRemaining()) {
					p.getList().add(buf.getInt());
				}
				listCount -= bufferSize;
			}

			buf = ByteBuffer.allocate(listCount * (Integer.SIZE / Byte.SIZE));
			buf.clear();
			if (fc.read(buf) < 0) {
				return null;
			}
			buf.rewind();
			while (buf.hasRemaining()) {
				p.getList().add(buf.getInt());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return p;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		/*
		 * Your code here
		 */
		ByteBuffer buf = ByteBuffer.allocate(bufferSize
				* (Integer.SIZE / Byte.SIZE));
		try {
			buf.clear();
			PutIntInBuf(buf, fc, p.getTermId());
			PutIntInBuf(buf, fc, p.getList().size());

			Iterator<Integer> itr = p.getList().iterator();
			while (itr.hasNext()) {
				PutIntInBuf(buf, fc, itr.next().intValue());
			}
			FlushBuffer(buf, fc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
}
