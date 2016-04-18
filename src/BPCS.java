import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mitchell on 3/7/2016.
 */
public class BPCS {

	private static final byte[] SALT = {(byte)0x3b,(byte)0x12,(byte)0x44,(byte)0x61,
			(byte)0xec,(byte)0xc0,(byte)0xa1,(byte)0x82,
			(byte)0x4d, (byte)0x97, (byte)0x4d, (byte)0x3c,
			(byte)0x57, (byte)0xd9, (byte)0x94, (byte)0x52};

	private static final String CIPHER_ALG = "AES/GCM/PKCS5Padding";
	private static final String SEC_RANDOM_ALG = "SHA1PRNG";
	private static final String S_KEY_FACTORY_ALG = "PBKDF2WithHmacSHA1";

	private static final int AES_KEY_SIZE = 128; //num bits
	private static final int GCM_NONCE_LENGTH = 16; //num bytes
	private static final int GCM_TAG_LENGTH = 16; //num bytes
	private static final int KEY_SPEC_ROUNDS = 65536;

	private float minComplexity = 0.3f;

	public BPCS(){
		setMinComplexity(minComplexity);
	}

	public BPCS(float minComplexity){
		setMinComplexity(minComplexity);
	}

	private void setMinComplexity(float complexity){
		if(complexity < 0 || complexity > 1){
			throw new IllegalArgumentException("Complexity must be between 0.0f and 1.0f");
		}
		this.minComplexity = complexity;
	}

	public BufferedImage embed(BufferedImage sourceImage, File message, String password) throws IOException{

		BufferedImage stegoImage = ImageModifier.getCopy(sourceImage);

		List<Block> messageBlocks = encrypt(message, password);
		System.out.printf("Message blocks: %d%n", messageBlocks.size());
		Point startLocation = getStartLocation(sourceImage, password);
		int startX = (int)startLocation.getX();
		int startY = (int)startLocation.getY();

		int validWidth = (stegoImage.getWidth() / 8) * 8;
		int validHeight = (stegoImage.getHeight() / 8) * 8;
		//boolean starting = true;
		int messagePointer = 0;
		for(int i = 0; i < validWidth; i+=8){
			int currentX = (startX + i) % validWidth;
			for(int j = 0; j < validHeight; j+=8){
				int currentY = (startY + j) % validHeight;

				if(messagePointer >= messageBlocks.size()){
					break;
				}

				System.out.printf("Analyzing block (%d, %d)%n", currentX, currentY);
				if(currentX >= validWidth || currentY >= validHeight){
					System.out.printf("Block (%d, %d) extends out of bounds, skipping...%n", currentX, currentY);
					continue;
				}

				System.out.printf("Converting to CGC%n");
				//FIXME you're not even doing something with the return value dumbass
				ImageModifier.convertPBCToCGC(stegoImage, currentX, currentY, 8, 8);

				//going deep into the block
				for(int b = 7; b >= 0; b--){
					for(int c = Channel.RED.ordinal(); c <= Channel.BLUE.ordinal(); c++){

						if(messagePointer >= messageBlocks.size()){
							break;
						}

						Block testBlock = new Block(stegoImage, currentX, currentY, b, Channel.values()[c]);
						if(testBlock.getComplexity() >= minComplexity){

							System.out.printf("Block (%d, %d) @ bit %d " + Channel.values()[c].name() + " is a hit!%n", currentX, currentY, b);

							Block messageBlock = messageBlocks.get(messagePointer);
							messageBlock.copyMetadata(testBlock);
							if(messageBlock.getComplexity() < minComplexity){
								System.out.printf("Message block %d needs to be conjugated%n", messagePointer);
								messageBlock.conjugate();
							}
							ImageModifier.writeBlock(stegoImage, messageBlock);
							messagePointer++;
						}

					}
				}

				System.out.printf("Converting back to PBC%n");
				ImageModifier.convertCGCToPBC(stegoImage, currentX, currentY, 8, 8);
				//done with this block
			}
		}

		if(messagePointer < messageBlocks.size()){
			throw new IndexOutOfBoundsException("Cannot fit entire Message into Stego Image");
		}

		return stegoImage;
	}

	//FIXME somehow the cipher is truncating the message or something
	private List<Block> encrypt(File file, String password) throws IOException{

		Path path = Paths.get(file.toURI());
		byte[] messageBytes = Files.readAllBytes(path);
		System.out.printf("Message bytes: %d%n", messageBytes.length);
		byte[] messageName = path.getFileName().toString().getBytes();

		byte[] cipherBytes = {};
		byte[] iv = {};
		try{

			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(S_KEY_FACTORY_ALG);
			PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), SALT, KEY_SPEC_ROUNDS, AES_KEY_SIZE);
			SecretKey key = keyFactory.generateSecret(pbeKeySpec);
			System.out.printf("Secret key take 1: " + Arrays.toString(key.getEncoded()) + "%n");

			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");

			//FIXME this is not encrypting the message correctly
			Cipher cipher = Cipher.getInstance(CIPHER_ALG);
			final byte[] nonce = new byte[GCM_NONCE_LENGTH];
			SecureRandom random = SecureRandom.getInstanceStrong();
			random.nextBytes(nonce);
			GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
			iv = paramSpec.getIV();

			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, paramSpec);
			cipher.updateAAD(messageName);
			//cipher.update(messageBytes);
			cipherBytes = cipher.doFinal(messageBytes);

			//TESTING
			//trying to free up memory
			//messageBytes = null;

		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}catch(NoSuchPaddingException e){
			e.printStackTrace();
		}catch(InvalidKeyException e){
			e.printStackTrace();
		}catch(InvalidAlgorithmParameterException e){
			e.printStackTrace();
		}catch(InvalidKeySpecException e){
			e.printStackTrace();
		}catch(BadPaddingException e){
			e.printStackTrace();
		}catch(IllegalBlockSizeException e){
			e.printStackTrace();
		}

		System.out.printf("IV bytes: %d%n", iv.length);
		System.out.printf("Cipher bytes: %d%n", cipherBytes.length);
		System.out.printf("Cipher bytes: " + Arrays.toString(cipherBytes) + "%n");

		//FIXME somehow the cipher is shortening the message or something
		return blockifyBytes(cipherBytes, messageName, iv);
		//return blockifyBytes(messageBytes, messageName, iv);
	}

	public void extract(BufferedImage stegoImage, Path path, String password){
		Point startLocation = getStartLocation(stegoImage, password);
		int startX = (int)startLocation.getX();
		int startY = (int)startLocation.getY();

		int validWidth = (stegoImage.getWidth() / 8) * 8;
		int validHeight = (stegoImage.getHeight() / 8) * 8;
		//some kind of container
		boolean reading = true;
		while(reading){

		}

	}

	private File decrypt(byte[] cipherBytes, byte[] iv, String fileName, String password){
		return new File(fileName);
	}

	private Block getNextBlock(BufferedImage image, int x, int y, int b, int c){
		Block test = new Block(image, x, y, b, Channel.values()[c]);
		while(test.getComplexity() < minComplexity){

		}
		return test;

	}

	private List<Block> blockifyBytes(byte[] bytes, byte[] messageName, byte[] iv){
		List<Block> blocks = new LinkedList<Block>();

		blocks.addAll(Block.blockify(iv));
		blocks.addAll(Block.blockify(messageName));
		blocks.addAll(Block.blockify(bytes));

		return blocks;
	}

	//returns pixel coordinates of starting block location based on
	//the password given
	private Point getStartLocation(BufferedImage sourceImage, String password){
		int startLocation = 0;
		int range = (sourceImage.getWidth()/8) * (sourceImage.getHeight()/8);;
		try{
			SecureRandom random = SecureRandom.getInstance(SEC_RANDOM_ALG);
			random.setSeed(password.getBytes());
			startLocation = random.nextInt(range);
		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}
		//?????????????????????????????????
		int x = 8 * (startLocation % (sourceImage.getWidth()/8));
		int y = 8 * (startLocation / (sourceImage.getWidth()/8));
		Point point = new Point(x, y);
		return point;
	}

}
