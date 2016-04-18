import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.List;

/**
 * Created by Mitchell on 3/23/2016.
 */
public class ImageModifier {

	public static BufferedImage getCopy(BufferedImage sourceImage){
		//make a deep copy of the image
		ColorModel cm = sourceImage.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = sourceImage.copyData(null);
		BufferedImage copyImage = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		return copyImage;
	}

	private static int convertPBCToCGC(BufferedImage image, int x, int y){
		int pixelPBC = image.getRGB(x, y);
		int pixelCGC = 0;
		/*
		for(int i = 0; i < 4; i++){
			int bitMask = 1 << (8 * i);
			pixelCGC = pixelCGC ^ (pixelPBC & bitMask);
			for(int j = 1; j < 8; j++){
				int lastMask = bitMask;
				bitMask = bitMask << 1;
				pixelCGC = pixelCGC ^ ((pixelPBC & bitMask)^(pixelPBC & lastMask));
			}
		}
		*/

		int bitMask = 0x01010101;
		pixelCGC = pixelCGC ^ (pixelPBC & bitMask);
		for(int j = 1; j < 8; j++){
			int lastMask = bitMask;
			bitMask = bitMask << 1;
			pixelCGC = pixelCGC ^ ((pixelPBC & bitMask)^(pixelPBC & lastMask));
		}

		//make sure the alpha channel is opaque
		pixelCGC = pixelCGC | 0xff000000;

		//image.setRGB(x, y, pixelCGC);
		return pixelCGC;
	}

	private static int convertCGCToPBC(BufferedImage image, int x, int y){
		int pixelCGC = image.getRGB(x, y);
		int pixelPBC = 0;

		int bitMask = 0x01010101;
		pixelPBC = pixelPBC ^ (pixelCGC & bitMask);
		for(int j = 1; j < 8; j++){
			int lastMask = bitMask;
			bitMask = bitMask << 1;
			pixelPBC = pixelPBC ^ ((pixelCGC & bitMask)^(pixelPBC & lastMask));
		}

		//make sure the alpha channel is opaque
		pixelPBC = pixelPBC | 0xff000000;

		//image.setRGB(x, y, pixelPBC);
		return pixelPBC;
	}

	public static int[][] convertPBCToCGC(BufferedImage image, int x, int y, int w, int h){
		int[][] pixelArray = new int[w][h];
		for(int i = x; i < x + w; i++){
			if(i >= image.getWidth()){
				break;
			}
			for(int j = y; j < y + h; j++){
				if(j >= image.getHeight()){
					break;
				}
				pixelArray[i-x][j-y] = convertPBCToCGC(image, i, j);
				image.setRGB(x, y, pixelArray[i-x][j-y]);
			}
		}
		return pixelArray;
	}

	public static int[][] convertCGCToPBC(BufferedImage image, int x, int y, int w, int h){
		int[][] pixelArray = new int[w][h];
		for(int i = x; i < x + w; i++){
			if(i >= image.getWidth()){
				break;
			}
			for(int j = y; j < y + h; j++){
				if(j >= image.getHeight()){
					break;
				}
				pixelArray[i-x][j-y] = convertCGCToPBC(image, i, j);
				image.setRGB(x, y, pixelArray[i-x][j-y]);
			}
		}
		return pixelArray;
	}

	public static void writeBlock(BufferedImage image, Block block){
		int[][] intBlock = block.getIntBlock();
		for(int i = 0; i < intBlock.length; i++){
			for(int j = 0; j < intBlock[0].length; j++){
				int current = image.getRGB(i + block.getX(), j + block.getY());
				int updated = current | intBlock[i][j];
				image.setRGB(i + block.getX(), j + block.getY(), updated);
			}
		}
	}

}
