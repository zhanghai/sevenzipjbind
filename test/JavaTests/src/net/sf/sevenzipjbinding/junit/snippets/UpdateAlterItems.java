package net.sf.sevenzipjbinding.junit.snippets;

/* BEGIN_SNIPPET(UpdateAlterItems) */
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItemAllFormats;
import net.sf.sevenzipjbinding.IOutUpdateArchive;
import net.sf.sevenzipjbinding.ISequentialInStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import net.sf.sevenzipjbinding.util.ByteArrayStream;

public class UpdateAlterItems {
    /**
     * The callback provides information about archive items.
     */
    private final class MyCreateCallback //
            implements IOutCreateCallback<IOutItemAllFormats> {

        public void setOperationResult(boolean operationResultOk)//
                throws SevenZipException {
            // Track each operation result here
        }

        public void setTotal(long total) throws SevenZipException {
            // Track operation progress here
        }

        public void setCompleted(long complete) throws SevenZipException {
            // Track operation progress here
        }

        public IOutItemAllFormats getItemInformation(int index,//
                OutItemFactory<IOutItemAllFormats> outItemFactory) //
                throws SevenZipException {
            if (index != 1) {
                // Keep properties and content
                return outItemFactory.createOutItem(index);
            }

            // Change property PATH and content
            IOutItemAllFormats item;
            item = outItemFactory.createOutItemAndCloneProperties(index);

            item.setUpdateIsNewData(true);
            item.setDataSize((long) /*sf*/NEW_CONTENT/**/.length);

            item.setUpdateIsNewProperties(true);
            item.setPropertyPath("new_name.txt");

            return item;
        }

        public ISequentialInStream getStream(int i) throws SevenZipException {
            if (i != 1) {
                return null;
            }
            return new ByteArrayStream(/*sf*/NEW_CONTENT/**/, true);
        }
    }

    static final byte[] NEW_CONTENT = "new content".getBytes();

    public static void main(String[] args) {
        if (args./*f*/length/* */== 2) {
            new UpdateAlterItems().compress(args[0], args[1]);
            return;
        }
        System.out.println("Usage: java UpdateAlterItems <in-arc> <out-arc>");
    }


    private void compress(String in, String out) {
        boolean success = false;
        RandomAccessFile inRaf = null;
        RandomAccessFile outRaf = null;
        IInArchive inArchive;
        IOutUpdateArchive<IOutItemAllFormats> outArchive = null;
        List<Closeable> closeables = new ArrayList<Closeable>();
        try {
            // Open input file
            inRaf = new RandomAccessFile(in, "r");
            closeables.add(inRaf);
            IInStream inStream = new RandomAccessFileInStream(inRaf);

            // Open in-archive
            inArchive = SevenZip.openInArchive(null, inStream);
            closeables.add(inArchive);

            outRaf = new RandomAccessFile(out, "rw");

            // Open out-archive object
            outArchive = inArchive.getConnectedOutArchive();

            // Alter archive
            outArchive.updateItems(new RandomAccessFileOutStream(outRaf),//
                    inArchive.getNumberOfItems(), new MyCreateCallback());

            success = true;
        } catch (SevenZipException e) {
            System.err.println("7z-Error occurs:");
            // Get more information using extended method
            e.printStackTraceExtended();
        } catch (Exception e) {
            System.err.println("Error occurs: " + e);
        } finally {
            for (int i = closeables.size() - 1; i >= 0; i--) {
                try {
                    closeables.get(i).close();
                } catch (IOException e) {
                    System.err.println("Error closing resource: " + e);
                    success = false;
                }
            }
        }
        if (success) {
            System.out.println("Update successful");
        }
    }
}
/* END_SNIPPET */