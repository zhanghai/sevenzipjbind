import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

public class ExtractItemsStandard {
    public static class MyExtractCallback implements IArchiveExtractCallback {
        private int hash = 0;
        private int /*f*/index/**/;
        private ISevenZipInArchive /*f*/inArchive/**/;

        public MyExtractCallback(ISevenZipInArchive inArchive) {
            this.inArchive = inArchive;
        }

        public ISequentialOutStream getStream(int index, 
                ExtractAskMode extractAskMode) throws SevenZipException {
            this.index = index;
            return new ISequentialOutStream() {

                public int write(byte[] data) throws SevenZipException {
                    hash |= Arrays.hashCode(data);
                    return data./*f*/length/**/; // Return amount of proceed data
                }
            };
        }

        public void prepareOperation(ExtractAskMode extractAskMode) 
                throws SevenZipException {
        }

        public void setOperationResult(ExtractOperationResult 
                extractOperationResult) throws SevenZipException {
            if (extractOperationResult != ExtractOperationResult./*sf*/OK/**/) {
                System.err.println("Extraction error");
            } else {
                System.out.println(String.format("%9X | %s", // 
                        /*f*/hash/**/, /*f*/inArchive/**/.getProperty(/*f*/index/**/, PropID./*sf*/PATH/**/)));
                hash = 0;
            }
        }

        public void setCompleted(long completeValue) throws SevenZipException {
        }

        public void setTotal(long total) throws SevenZipException {
        }

    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java ExtractItemsStandard <arch-name>");
        }
        RandomAccessFile randomAccessFile = null;
        ISevenZipInArchive inArchive = null;
        try {
            randomAccessFile = new RandomAccessFile(args[0], "r");
            inArchive = SevenZip.openInArchive(null, // autodetect archive type
                    new RandomAccessFileInStream(randomAccessFile));

            System.out.println("   Hash   | Filename");
            System.out.println("----------+---------");

            int count = inArchive.getNumberOfItems();
            List<Integer> itemsToExtract = new ArrayList<Integer>();
            for (int i = 0; i < count; i++) {
                if (!((Boolean) inArchive.getProperty(i, PropID./*sf*/IS_FOLDER/**/))
                        .booleanValue()) {
                    itemsToExtract.add(Integer.valueOf(i));
                }
            }
            int[] items = new int[itemsToExtract.size()];
            int i = 0;
            for (Integer integer : itemsToExtract) {
                items[i++] = integer.intValue();
            }
            inArchive.extract(items, false, // Non-test mode
                    new MyExtractCallback(inArchive));
        } catch (Exception e) {
            System.err.println("Error occurs: " + e);
            System.exit(1);
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {
                    System.err.println("Error closing archive: " + e);
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    System.err.println("Error closing file: " + e);
                }
            }
        }
    }
}