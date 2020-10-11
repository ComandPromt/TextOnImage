package gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.ImageIcon;

import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GeoTiffTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

public class AppWindow {

	private JFrame frame;
	//Create a file chooser
	final JFileChooser fc = new JFileChooser();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AppWindow window = new AppWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AppWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		//open file chooser
		File file = null;
		//fc.setMultiSelectionEnabled(true);
		//TODO rewrite to iterate an array of files
		//set what types of files to display in chooser
		fc.setFileFilter(new FileFilter(){
			public boolean accept(File file) {
				if(file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".jpeg")) {
					return true;}
				return false;
			}
			public String getDescription() {
				return "JPG and JPEG images";
			}
			});
		int returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            //anything else we want to do with the file
        } else {
            //action on cancel, probably nothing
        }
		
		BufferedImage image = null;
        try
        {
          image = ImageIO.read(file);
//          image = Imaging.getBufferedImage(file); //different library, not sure if different, use if there are problems
        }
        catch (Exception e)
        {
        	//failed to make image, handle exception here
          e.printStackTrace();
          System.exit(1);
        }
        Graphics2D gImage = image.createGraphics();
        
        //get the byte array of the original file of the image so that we can use it directly to find the EXIF data. Assumes the image is a jpeg
        byte imageBytes[]=null;
		try {
			imageBytes = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//use Apache library to calculate the metadata and convert it to a TIFF style EXIF 
		ImageMetadata metadata = null;
        try {
			metadata = Imaging.getMetadata(imageBytes);
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        final TiffImageMetadata exif = jpegMetadata.getExif();
        
        //Extract GPS metadata from the image
        TiffImageMetadata.GPSInfo gpsInfo = null;
        try {
			gpsInfo = exif.getGPS();
		} catch (ImageReadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        //Construct GPS Coord String
        DecimalFormat df = new DecimalFormat("0.00000");
        String coords = null;
        try {
			coords = df.format(Math.abs(gpsInfo.getLatitudeAsDegreesNorth())) + "� " + gpsInfo.latitudeRef + ", " + df.format(Math.abs(gpsInfo.getLongitudeAsDegreesEast())) + "� " + gpsInfo.longitudeRef;
		} catch (ImageReadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        //construct the date string
        DateFormat dateFormat = new SimpleDateFormat("''yyyy:MM:dd HH:mm:ss''", Locale.ENGLISH);
        DateFormat dateRenderFormat = new SimpleDateFormat("dd MMM yyyy h:mm a", Locale.ENGLISH);
        Date imageDate = null;
        try {
			imageDate = dateFormat.parse(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        String date = dateRenderFormat.format(imageDate);
        
        
        //print gps and date info to SYSO for debugging reasons, remove in final release. TODO remove when done
        System.out.println(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
        System.out.println(date);
        System.out.println(gpsInfo.latitudeDegrees + "� " + gpsInfo.latitudeMinutes + "\' " + gpsInfo.latitudeSeconds.doubleValue() + "\" " + gpsInfo.latitudeRef);
        System.out.println(gpsInfo.longitudeDegrees + "� " + gpsInfo.longitudeMinutes + "\' " + gpsInfo.longitudeSeconds.doubleValue() + "\" " + gpsInfo.longitudeRef);
        System.out.println(coords);
        
        
        
        TiffOutputSet outputSet = null;
        try {
			outputSet = exif.getOutputSet();
		} catch (ImageWriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//        TiffOutputDirectory gpsCoords=null;
//        gpsCoords = outputSet.getGPSDirectory();
//        
//        System.out.println(gpsCoords.findField(2));
//        System.out.println(gpsCoords.findField(4));
//        TiffOutputField field = gpsCoords.findField(2);
        
        //setup font stuff so we can write on the image
        Font overlayFont = new Font(Font.SANS_SERIF,Font.BOLD,70);
        gImage.setFont(overlayFont);
        //suggest that it use antiAliaing since this is text
        gImage.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        gImage.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        //draw date
        //create shape of the outer edge of the text. This will allow us to draw the outline in one color and then fill with solid color
        TextLayout textLayout = new TextLayout(date,overlayFont,new FontRenderContext(null,false,false)); //turn string into style we want for rendering
        AffineTransform transform = new AffineTransform();
        double startPoint = image.getHeight()*0.97 - textLayout.getAscent()*3.3;//where the line of text should be, start at 3 lines above the 97% mark 
        transform.translate(image.getWidth()*0.03, startPoint);//set location of outline for rendering in the bottom of the image
        startPoint = startPoint + textLayout.getAscent()*1.1;//drop down a line
        Shape outline = textLayout.getOutline(transform); //get the outer edges of the text
        //Draw a string on the image
        //gImage.drawString(coords, 100, 100);//string does not quite line up if you use this, using fill instead.
        gImage.setColor(Color.BLACK);
        gImage.setStroke(new BasicStroke(10));
        gImage.draw(outline);//adds the outline
        gImage.setColor(Color.YELLOW);
        gImage.fill(outline); //fill in the outline with color
        
        //draw coords
        //create shape of the outer edge of the text. This will allow us to draw the outline in one color and then fill with solid color
        textLayout = new TextLayout(coords,overlayFont,new FontRenderContext(null,false,false)); //turn string into style we want for rendering
        transform = new AffineTransform();
        transform.translate(image.getWidth()*0.03, startPoint);//set location of outline for rendering in the bottom of the image
        startPoint = startPoint + textLayout.getAscent()*1.1;//drop down a line
        outline = textLayout.getOutline(transform); //get the outer edges of the text
        //Draw a string on the image
        //gImage.drawString(coords, 100, 100);//string does not quite line up if you use this, using fill instead.
        gImage.setColor(Color.BLACK);
        gImage.setStroke(new BasicStroke(10));
        gImage.draw(outline);//adds the outline
        gImage.setColor(Color.YELLOW);
        gImage.fill(outline); //fill in the outline with color
        
        //save image
        returnVal = fc.showOpenDialog(frame);
        frame.getContentPane().setLayout(new GridLayout(1, 0, 0, 0));
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            //anything else we want to do with the file
        } else {
            //action on cancel, probably nothing
        }
		
		//create and image writer and set it to the quality settings we want
		final ImageWriter imWrite = ImageIO.getImageWritersByFormatName("jpg").next();
		JPEGImageWriteParam jpgParams = new JPEGImageWriteParam(null);
		jpgParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgParams.setCompressionQuality(0.95f);//set quality to 90% using float
		
		//set location for writer by turning our file into an output stream and passing that to setOutput so we can close it later.
		FileImageOutputStream fileOutStream = null;
		try {
			fileOutStream = new FileImageOutputStream(file);
		} catch (IOException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
		imWrite.setOutput(fileOutStream);
        try {
        	imWrite.write(null,new IIOImage(image,null,null),jpgParams);//write image using params set above. Still need to add EXIF data
        	fileOutStream.close();//close the file to release locks
//			ImageIO.write(image, "jpg", file); //TODO this method blows away existing EXIF data, find way to preserve. Write exif back to image after making?
//			Imaging.writeImage(image, file, ImageFormats.JPEG, null); //library does not support JPG writing so can not use this
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        
        //add EXIF data using Apache library
        try {
			new ExifRewriter().updateExifMetadataLossless(Files.readAllBytes(file.toPath()), new FileOutputStream(file), outputSet);
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ImageWriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}

}
