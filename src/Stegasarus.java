import javax.crypto.IllegalBlockSizeException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Mitchell on 3/3/2016.
 */
public class Stegasarus {

    public static void main(String[] args){
		Scanner input = new Scanner(System.in);

		System.out.println("Please type the path of the cover file: ");
		String sourceFilePath = input.nextLine();
        BufferedImage sourceImg = null;
        try{
			sourceImg = ImageIO.read(new File(sourceFilePath));
		}catch(IOException e){
			System.out.println("Could not find cover file");
			e.printStackTrace();
		}
		System.out.println("Please type the path of the message file: ");
		String messageFilePath = input.nextLine();
		File messageFile = new File(messageFilePath);

		int dot = sourceFilePath.lastIndexOf('.');
		String extension = sourceFilePath.substring(dot + 1);
		String stegoFilePath = sourceFilePath.substring(0, dot) + "[STEGO]" + sourceFilePath.substring(dot);
		System.out.println("Preparing to write stegoImage to: " + stegoFilePath);
		System.out.println("Please type your password: ");
		String password = input.nextLine();
		BPCS bpcs = new BPCS();
		try{
			BufferedImage stegoImage = bpcs.embed(sourceImg, messageFile, password);
			File stegoImg = new File(stegoFilePath);
			ImageIO.write(stegoImage, extension, stegoImg);
		}catch(IOException e){
			System.out.println("Could not write stego file");
			e.printStackTrace();
		}


	}

}
