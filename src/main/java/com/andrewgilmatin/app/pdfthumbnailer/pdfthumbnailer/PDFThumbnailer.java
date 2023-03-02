package com.andrewgilmatin.app.pdfthumbnailer.pdfthumbnailer;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import java.text.MessageFormat;

public class PDFThumbnailer {

    public static Logger logger = Logger.getLogger(PDFThumbnailer.class.getName());

    public static void main(final String[] args) throws Exception {

        for ( int fileNumber = 0; fileNumber < args.length; fileNumber++ ) {
            File infile = new File(args[fileNumber]);
            logger.info( MessageFormat.format( "processing file {0}", infile.getAbsolutePath() ) );
            RandomAccessFile raf = new RandomAccessFile(infile, "r");

            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile = new PDFFile(buf);

            String readableFormat = "page-{0,number,0000}.png";
            String thumbnailFormat = "page-{0,number,0000}-thumbnail.png";

            for ( int pageNumber = 1; pageNumber <= pdffile.getNumPages(); pageNumber++ ) {
                logger.info( MessageFormat.format( "processing page {0}", new Integer(pageNumber) ) );

                PDFPage page = pdffile.getPage(pageNumber);

                Dimension pageSize = page.getAspectRatio() < 1
                    ? new Dimension( (int)page.getBBox().getWidth(), (int)page.getBBox().getHeight() )
                    : new Dimension( (int)page.getBBox().getHeight(), (int)page.getBBox().getWidth() );
                Rectangle pageBox = new Rectangle(0,0, pageSize.width, pageSize.height );

                {
                    Dimension readableSize = page.getAspectRatio() < 1
                        ? page.getUnstretchedSize( 850, Integer.MAX_VALUE, pageBox  ) // 8.5"
                        : page.getUnstretchedSize( Integer.MAX_VALUE, 1100, pageBox ) // 11"
                    ;
                    BufferedImage img = (BufferedImage)page.getImage(
                        readableSize.width,
                        readableSize.height,
                        pageBox, // clip rect
                        null, // null for the ImageObserver
                        true, // fill background with white
                        true  // block until drawing is done
                    );

                    File outfile = new File( MessageFormat.format( readableFormat, new Integer(pageNumber) ) );
                    logger.info( MessageFormat.format( "writting file {0}", outfile.getAbsolutePath() ) );
                    ImageIO.write(img, "PNG", outfile);

                    img.flush();
                    img = null;
                }

                {
                    Dimension thumbnailSize = page.getAspectRatio() < 1
                        ? page.getUnstretchedSize( 100, Integer.MAX_VALUE, pageBox ) // 8.5" (8.5/0.085)
                        : page.getUnstretchedSize( Integer.MAX_VALUE, 129, pageBox ) // 11" (11/0.085)
                    ;

                    BufferedImage img = (BufferedImage)page.getImage(
                        thumbnailSize.width,
                        thumbnailSize.height,
                        pageBox, // clip rect
                        null, // null for the ImageObserver
                        true, // fill background with white
                        true  // block until drawing is done
                    );

                    File outfile = new File( MessageFormat.format( thumbnailFormat, new Integer(pageNumber) ) );
                    logger.info( MessageFormat.format( "writting file {0}", outfile.getAbsolutePath() ) );
                    ImageIO.write(img, "PNG", outfile);

                    img.flush();
                    img = null;
                }
            }
            channel.close();
        }
    }
}

// END